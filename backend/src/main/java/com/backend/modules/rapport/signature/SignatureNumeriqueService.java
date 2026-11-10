package com.backend.modules.rapport.signature;

import com.backend.common.exception.BusinessRuleException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.ess.ESSCertIDv2;
import org.bouncycastle.asn1.ess.SigningCertificateV2;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Signature électronique avancée des rapports au format PAdES (PDF + CMS détaché) :
 * <ul>
 *   <li>un certificat X.509 par laboratoire, créé à la première signature et conservé
 *       dans un coffre PKCS#12 ({@code <stockage>/certificats/<schéma>.p12}) ;</li>
 *   <li>attribut signing-certificate-v2 exigé par PAdES (niveau B-B) ;</li>
 *   <li>horodatage RFC 3161 par une autorité externe (FreeTSA par défaut, niveau B-T).
 *       En cas d'indisponibilité, le rapport est signé sans horodatage.</li>
 * </ul>
 * Le certificat est auto-signé : le PDF est protégé contre toute modification,
 * mais l'identité du laboratoire n'est pas certifiée par une autorité reconnue.
 */
@Slf4j
@Service
public class SignatureNumeriqueService {

    private static final String ALIAS = "labflow";
    private static final String ALGORITHME = "SHA256withRSA";

    @Value("${app.storage.path:${user.home}/lims-documents}")
    private String stockage;

    @Value("${app.signature.keystore-password:labflow-dev-keystore}")
    private String motDePasseCoffre;

    @Value("${app.signature.tsa-enabled:true}")
    private boolean horodatageActif;

    @Value("${app.signature.tsa-url:https://freetsa.org/tsr}")
    private String urlHorodatage;

    @Value("${app.signature.tsa-timeout-ms:10000}")
    private long delaiHorodatageMs;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    /** Résultat d'une signature. */
    public record PdfSigne(byte[] contenu, Instant horodatage) {
    }

    /** Résultat de la vérification cryptographique d'un PDF. */
    public record Verification(boolean signaturePresente, boolean signatureValide, boolean documentComplet,
                               String signataire, String certificat, Instant dateSignature, Instant horodatage) {
    }

    /**
     * Signe le PDF au nom du laboratoire.
     *
     * @param schema    schéma du laboratoire, sert de nom au coffre de certificats
     * @param laboratoire raison sociale, inscrite dans le certificat
     */
    public PdfSigne signer(byte[] pdf, String schema, String laboratoire, String signataire,
                           String lieu, Instant dateSignature) {
        Identite identite = identite(schema, laboratoire);
        Instant[] horodatage = new Instant[1];
        try (PDDocument document = PDDocument.load(pdf);
             ByteArrayOutputStream sortie = new ByteArrayOutputStream();
             SignatureOptions options = new SignatureOptions()) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ETSI_CADES_DETACHED);
            signature.setName(signataire);
            signature.setLocation(lieu);
            signature.setReason("Validation et libération du rapport d'analyse");
            Calendar date = Calendar.getInstance();
            date.setTimeInMillis(dateSignature.toEpochMilli());
            signature.setSignDate(date);
            options.setPreferredSignatureSize(32_768);
            SignatureInterface signeur = contenu -> {
                try {
                    byte[] cms = signerCms(contenu, identite);
                    if (horodatageActif) {
                        CmsHorodate resultat = horodater(cms);
                        horodatage[0] = resultat.date();
                        return resultat.cms();
                    }
                    return cms;
                } catch (GeneralSecurityException | CMSException | OperatorCreationException ex) {
                    throw new IOException("Signature CMS impossible : " + ex.getMessage(), ex);
                }
            };
            document.addSignature(signature, signeur, options);
            document.saveIncremental(sortie);
            return new PdfSigne(sortie.toByteArray(), horodatage[0]);
        } catch (IOException ex) {
            log.error("Échec de la signature PAdES : {}", ex.getMessage());
            throw new BusinessRuleException("SIGNATURE_ECHOUEE", "La signature électronique du PDF a échoué.");
        }
    }

    /** Vérifie la signature d'un PDF : intégrité, couverture complète du fichier et horodatage. */
    public Verification verifier(byte[] fichier) {
        try (PDDocument document = PDDocument.load(fichier)) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            if (signatures.isEmpty()) {
                return new Verification(false, false, false, null, null, null, null);
            }
            PDSignature signature = signatures.getLast();
            int[] plage = signature.getByteRange();
            boolean complet = plage != null && plage.length == 4 && (long) plage[2] + plage[3] == fichier.length;
            byte[] contenuSigne = signature.getSignedContent(fichier);
            CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(contenuSigne), signature.getContents(fichier));
            SignerInformation signerInfo = cms.getSignerInfos().getSigners().iterator().next();
            @SuppressWarnings("unchecked")
            Collection<X509CertificateHolder> certificats = cms.getCertificates().getMatches(signerInfo.getSID());
            X509CertificateHolder certificat = certificats.iterator().next();
            boolean valide = signerInfo.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certificat));
            Instant horodatage = null;
            AttributeTable nonSignes = signerInfo.getUnsignedAttributes();
            if (nonSignes != null && nonSignes.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken) != null) {
                Attribute attribut = nonSignes.get(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken);
                TimeStampToken jeton = new TimeStampToken(new CMSSignedData(
                        attribut.getAttrValues().getObjectAt(0).toASN1Primitive().getEncoded()));
                horodatage = jeton.getTimeStampInfo().getGenTime().toInstant();
            }
            return new Verification(true, valide, complet, signature.getName(), certificat.getSubject().toString(),
                    signature.getSignDate() == null ? null : signature.getSignDate().toInstant(), horodatage);
        } catch (Exception ex) {
            log.warn("Vérification de signature impossible : {}", ex.getMessage());
            return new Verification(true, false, false, null, null, null, null);
        }
    }

    // -------------------------------------------------------------------------
    // CMS (CAdES détaché) et horodatage
    // -------------------------------------------------------------------------

    private byte[] signerCms(InputStream contenu, Identite identite)
            throws GeneralSecurityException, CMSException, OperatorCreationException, IOException {
        X509Certificate certificat = identite.certificat();
        ContentSigner signeur = new JcaContentSignerBuilder(ALGORITHME).build(identite.cle());
        byte[] empreinteCertificat = MessageDigest.getInstance("SHA-256").digest(certificat.getEncoded());
        SigningCertificateV2 certificatSignataire = new SigningCertificateV2(new ESSCertIDv2(
                new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256), empreinteCertificat));
        ASN1EncodableVector attributs = new ASN1EncodableVector();
        attributs.add(new Attribute(PKCSObjectIdentifiers.id_aa_signingCertificateV2, new DERSet(certificatSignataire)));
        AttributeTable table = new AttributeTable(attributs);
        // PAdES : la date de signature figure dans le dictionnaire PDF, pas dans le CMS.
        CMSAttributeTableGenerator generateur = parametres -> {
            AttributeTable produite = new DefaultSignedAttributeTableGenerator(table).getAttributes(parametres);
            return produite.remove(CMSAttributes.signingTime);
        };
        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
        gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build())
                .setSignedAttributeGenerator(generateur)
                .build(signeur, certificat));
        gen.addCertificates(new JcaCertStore(List.of(certificat)));
        CMSSignedData signe = gen.generate(new CMSProcessableByteArray(contenu.readAllBytes()), false);
        return signe.getEncoded();
    }

    private record CmsHorodate(byte[] cms, Instant date) {
    }

    /** Ajoute un jeton d'horodatage RFC 3161 sur la valeur de signature (attribut non signé). */
    private CmsHorodate horodater(byte[] cms) throws CMSException, IOException {
        CMSSignedData signe = new CMSSignedData(cms);
        SignerInformation signerInfo = signe.getSignerInfos().getSigners().iterator().next();
        try {
            byte[] empreinte = MessageDigest.getInstance("SHA-256").digest(signerInfo.getSignature());
            TimeStampRequestGenerator generateur = new TimeStampRequestGenerator();
            generateur.setCertReq(true);
            TimeStampRequest requete = generateur.generate(TSPAlgorithms.SHA256, empreinte,
                    new BigInteger(64, new SecureRandom()));
            HttpResponse<byte[]> reponse = http.send(HttpRequest.newBuilder(URI.create(urlHorodatage))
                            .timeout(Duration.ofMillis(delaiHorodatageMs))
                            .header("Content-Type", "application/timestamp-query")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(requete.getEncoded()))
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (reponse.statusCode() != 200) {
                throw new IOException("réponse HTTP " + reponse.statusCode());
            }
            TimeStampResponse tsr = new TimeStampResponse(reponse.body());
            tsr.validate(requete);
            TimeStampToken jeton = tsr.getTimeStampToken();
            if (jeton == null) {
                throw new IOException("jeton absent : " + tsr.getStatusString());
            }
            ASN1EncodableVector nonSignes = new ASN1EncodableVector();
            nonSignes.add(new Attribute(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
                    new DERSet(ASN1Primitive.fromByteArray(jeton.getEncoded()))));
            SignerInformation horodate = SignerInformation.replaceUnsignedAttributes(signerInfo, new AttributeTable(nonSignes));
            CMSSignedData resultat = CMSSignedData.replaceSigners(signe, new SignerInformationStore(horodate));
            return new CmsHorodate(resultat.getEncoded(), jeton.getTimeStampInfo().getGenTime().toInstant());
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Horodatage {} indisponible, signature sans horodatage : {}", urlHorodatage, ex.getMessage());
            return new CmsHorodate(cms, null);
        }
    }

    // -------------------------------------------------------------------------
    // Coffre de certificats du laboratoire
    // -------------------------------------------------------------------------

    private record Identite(PrivateKey cle, X509Certificate certificat) {
    }

    private synchronized Identite identite(String schema, String laboratoire) {
        Path coffre = Path.of(stockage).toAbsolutePath().normalize()
                .resolve("certificats").resolve(schema.replaceAll("[^a-z0-9_]", "_") + ".p12");
        char[] motDePasse = motDePasseCoffre.toCharArray();
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            if (Files.exists(coffre)) {
                try (InputStream entree = Files.newInputStream(coffre)) {
                    ks.load(entree, motDePasse);
                }
                X509Certificate certificat = (X509Certificate) ks.getCertificate(ALIAS);
                if (certificat.getNotAfter().toInstant().isAfter(Instant.now())) {
                    return new Identite((PrivateKey) ks.getKey(ALIAS, motDePasse), certificat);
                }
                log.info("Certificat de signature expiré pour {}, renouvellement", schema);
            }
            ks.load(null, null);
            KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
            generateur.initialize(3072);
            KeyPair paire = generateur.generateKeyPair();
            X509Certificate certificat = certificatAutoSigne(paire, laboratoire);
            ks.setKeyEntry(ALIAS, paire.getPrivate(), motDePasse, new java.security.cert.Certificate[]{certificat});
            Files.createDirectories(coffre.getParent());
            try (OutputStream sortie = Files.newOutputStream(coffre)) {
                ks.store(sortie, motDePasse);
            }
            log.info("Certificat de signature créé pour le laboratoire {} ({})", laboratoire, coffre);
            return new Identite(paire.getPrivate(), certificat);
        } catch (Exception ex) {
            log.error("Coffre de certificats inutilisable pour {} : {}", schema, ex.getMessage());
            throw new BusinessRuleException("CERTIFICAT_INDISPONIBLE",
                    "Le certificat de signature du laboratoire est inutilisable. Vérifiez APP_SIGNATURE_KEYSTORE_PASSWORD.");
        }
    }

    private static X509Certificate certificatAutoSigne(KeyPair paire, String laboratoire) throws Exception {
        String nom = laboratoire == null || laboratoire.isBlank() ? "Laboratoire LabFlow" : laboratoire;
        X500Name sujet = new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, nom)
                .addRDN(BCStyle.O, nom)
                .addRDN(BCStyle.OU, "Responsable du laboratoire")
                .addRDN(BCStyle.C, "MA")
                .build();
        Instant debut = Instant.now().minus(1, ChronoUnit.DAYS);
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(sujet,
                new BigInteger(64, new SecureRandom()), Date.from(debut),
                Date.from(debut.plus(5 * 365, ChronoUnit.DAYS)), sujet, paire.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
        ContentSigner signeur = new JcaContentSignerBuilder(ALGORITHME).build(paire.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signeur));
    }
}
