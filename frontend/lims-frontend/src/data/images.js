/**
 * Photos libres de droits (Unsplash), servies via URLs directes.
 * Chaque image reçoit un voile mint/sarcelle via l'utilitaire `photo-tint`.
 */
const base = "https://images.unsplash.com/";
const src = (id, w = 1200) => `${base}${id}?auto=format&fit=crop&w=${w}&q=80`;
export const photos = {
    teamLab: {
        src: src("photo-1581093450021-4a7360e9a6b5", 1400),
        alt: "Équipe de techniciens de laboratoire en blouse blanche devant des instruments d'analyse",
    },
    technicianMicroscope: {
        src: src("photo-1582719471384-894fbb16e074", 1200),
        alt: "Technicienne de laboratoire observant un échantillon au microscope",
    },
    labCorridor: {
        src: src("photo-1576086213369-97a306d36557", 1200),
        alt: "Plateau technique d'un laboratoire d'analyses avec automates alignés",
    },
    microscopes: {
        src: src("photo-1518152006812-edab29b069ac", 1000),
        alt: "Rangée de microscopes optiques sur un plan de travail de laboratoire",
    },
    sampling: {
        src: src("photo-1615461066159-fea0960485d5", 1000),
        alt: "Prélèvement d'un échantillon sanguin par un technicien portant des gants",
    },
    cells: {
        src: src("photo-1532938911079-1b06ac7ceec7", 1000),
        alt: "Vue microscopique de cellules marquées par fluorescence",
    },
    scientistLab: {
        src: src("photo-1579154204601-01588f351e67", 1200),
        alt: "Technicien de laboratoire manipulant des éprouvettes sous une hotte",
    },
};
