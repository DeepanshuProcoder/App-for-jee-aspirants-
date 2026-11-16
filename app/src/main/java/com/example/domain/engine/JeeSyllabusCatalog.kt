package com.example.domain.engine

import com.example.domain.model.Chapter
import com.example.domain.model.ChemistrySection
import com.example.domain.model.ClassLevel
import com.example.domain.model.Subject
import com.example.domain.model.Topic

data class SeedChapterSpec(
    val id: String,
    val name: String,
    val subject: Subject,
    val section: ChemistrySection,
    val classLevel: ClassLevel,
    val orderIndex: Int,
    val importance: Int,
    val topics: List<String>
)

/**
 * Editable JEE Main & Advanced Syllabus Catalogue (Class 11 & Class 12).
 * All seeded chapters and topics start at NOT_STARTED with zero fabricated progress.
 */
object JeeSyllabusCatalog {

    const val DEFAULT_SYLLABUS_VERSION = "JEE-2026-v1"

    val defaultSpecs: List<SeedChapterSpec> = listOf(
        // ================= PHYSICS (CLASS 11) =================
        SeedChapterSpec(
            "phy_11_01", "Units and Dimensions", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 1, 3,
            listOf("SI Units & Fundamental Quantities", "Dimensional Analysis", "Error Analysis & Propagation", "Vernier Calipers & Screw Gauge", "Significant Figures")
        ),
        SeedChapterSpec(
            "phy_11_02", "Mathematical Tools", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 2, 4,
            listOf("Trigonometry & Small Angle Approximations", "Differential Calculus in Physics", "Integral Calculus & Area", "Maxima, Minima & Graphs")
        ),
        SeedChapterSpec(
            "phy_11_03", "Vectors", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 3, 4,
            listOf("Vector Addition & Resolution", "Dot Product & Projections", "Cross Product & Torque Sense", "Unit Vectors & Relative Directions")
        ),
        SeedChapterSpec(
            "phy_11_04", "Motion in a Straight Line", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 4, 4,
            listOf("Distance vs Displacement", "Instantaneous Velocity & Acceleration", "Equations of Uniformly Accelerated Motion", "Motion Under Gravity", "Kinematics Graphs (x-t, v-t, a-t)", "Relative Motion in 1D")
        ),
        SeedChapterSpec(
            "phy_11_05", "Motion in a Plane", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 5, 4,
            listOf("Projectile on Horizontal Plane", "Projectile on Inclined Plane", "Relative Motion in 2D (Rain-Man, River-Boat)", "Uniform & Non-Uniform Circular Motion")
        ),
        SeedChapterSpec(
            "phy_11_06", "Laws of Motion", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 6, 5,
            listOf("Newton's First Law", "Newton's Second Law", "Newton's Third Law", "Free Body Diagrams", "Friction", "Connected Bodies", "Pulley Systems", "Inclined Planes", "Pseudo Force & Non-Inertial Frames", "Dynamics of Circular Motion")
        ),
        SeedChapterSpec(
            "phy_11_07", "Work, Energy and Power", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 7, 5,
            listOf("Work Done by Constant & Variable Forces", "Work-Energy Theorem", "Conservative Forces & Potential Energy", "Conservation of Mechanical Energy", "Vertical Circular Motion", "Power & Efficiency")
        ),
        SeedChapterSpec(
            "phy_11_08", "Centre of Mass", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 8, 4,
            listOf("COM of Discrete & Continuous Bodies", "Motion of COM", "Conservation of Linear Momentum", "Elastic & Inelastic Collisions", "Variable Mass Systems & Rocket")
        ),
        SeedChapterSpec(
            "phy_11_09", "Rotational Motion", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 9, 5,
            listOf("Moment of Inertia & Theorems", "Torque & Equilibrium", "Angular Momentum Conservation", "Pure Rolling Motion", "Rolling on Inclined Plane", "Instantaneous Axis of Rotation", "Toppling & Angular Impulse")
        ),
        SeedChapterSpec(
            "phy_11_10", "Gravitation", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 10, 4,
            listOf("Newton's Law & Field Intensity", "Gravitational Potential & Energy", "Variation of g", "Escape & Orbital Velocity", "Kepler's Laws & Satellites")
        ),
        SeedChapterSpec(
            "phy_11_11", "Properties of Solids and Fluids", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 11, 4,
            listOf("Stress, Strain & Young's Modulus", "Hydrostatic Pressure & Pascal's Law", "Archimedes Principle & Buoyancy", "Equation of Continuity & Bernoulli's Theorem", "Viscosity & Stokes Law", "Surface Tension & Capillarity")
        ),
        SeedChapterSpec(
            "phy_11_12", "Thermal Physics", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 12, 4,
            listOf("Thermal Expansion", "Calorimetry & Phase Change", "Conduction & Thermal Resistance", "Stefan-Boltzmann Law & Newton Cooling", "Wien's Displacement Law")
        ),
        SeedChapterSpec(
            "phy_11_13", "Thermodynamics", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 13, 5,
            listOf("Zeroth & First Law", "Isothermal, Adiabatic, Isobaric & Isochoric Processes", "Polytropic Processes & Molar Heat Capacity", "Carnot Engine & Second Law")
        ),
        SeedChapterSpec(
            "phy_11_14", "Kinetic Theory", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 14, 3,
            listOf("Ideal Gas Equation & Pressure", "RMS, Average & Most Probable Speed", "Degrees of Freedom & Equipartition", "Mean Free Path")
        ),
        SeedChapterSpec(
            "phy_11_15", "Oscillations", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 15, 4,
            listOf("SHM Equation & Phase", "Energy in SHM", "Spring-Block Combinations", "Simple & Physical Pendulum", "Damped & Forced Oscillations")
        ),
        SeedChapterSpec(
            "phy_11_16", "Waves", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 16, 4,
            listOf("Progressive Wave Equation", "Speed of Transverse & Sound Waves", "Standing Waves on Strings", "Organ Pipes & Resonance Tube", "Beats & Doppler Effect")
        ),

        // ================= PHYSICS (CLASS 12) =================
        SeedChapterSpec(
            "phy_12_01", "Electrostatics", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 17, 5,
            listOf("Coulomb's Law & Electric Field", "Gauss's Law & Flux", "Electric Potential & Equipotential Surfaces", "Electric Dipole", "Capacitors & Dielectrics")
        ),
        SeedChapterSpec(
            "phy_12_02", "Current Electricity", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 18, 5,
            listOf("Drift Velocity & Ohm's Law", "Kirchhoff's Laws & Symmetry", "RC Circuits", "Meter Bridge & Potentiometer")
        ),
        SeedChapterSpec(
            "phy_12_03", "Magnetism", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 19, 5,
            listOf("Biot-Savart Law & Ampere's Law", "Lorentz Force & Helical Motion", "Force on Current-Carrying Wire", "Magnetic Dipole & Earth Magnetism")
        ),
        SeedChapterSpec(
            "phy_12_04", "Electromagnetic Induction", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 20, 4,
            listOf("Faraday & Lenz Laws", "Motional EMF", "Self & Mutual Inductance", "LR Circuits & Induced Electric Field")
        ),
        SeedChapterSpec(
            "phy_12_05", "Alternating Current", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 21, 3,
            listOf("RMS & Phasors", "Series LCR & Resonance", "Power Factor & Transformers")
        ),
        SeedChapterSpec(
            "phy_12_06", "Electromagnetic Waves", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 22, 2,
            listOf("Displacement Current", "Wave Propagation & Poynting Vector", "EM Spectrum")
        ),
        SeedChapterSpec(
            "phy_12_07", "Ray Optics", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 23, 5,
            listOf("Plane & Spherical Mirrors", "Snell's Law & TIR", "Prism & Dispersion", "Thin Lenses & Silvered Lenses", "Optical Instruments")
        ),
        SeedChapterSpec(
            "phy_12_08", "Wave Optics", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 24, 4,
            listOf("Huygens Principle", "Young's Double Slit Experiment", "Thin Film Interference", "Diffraction & Polarisation")
        ),
        SeedChapterSpec(
            "phy_12_09", "Modern Physics", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 25, 5,
            listOf("Photoelectric Effect", "De Broglie Wavelength", "Bohr Atomic Model", "X-Rays", "Nuclear Physics & Radioactivity")
        ),
        SeedChapterSpec(
            "phy_12_10", "Semiconductors", Subject.PHYSICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 26, 3,
            listOf("Energy Bands & Intrinsic/Extrinsic", "p-n Junction & Zener Diode", "Logic Gates")
        ),

        // ================= CHEMISTRY: PHYSICAL =================
        SeedChapterSpec(
            "chem_phy_01", "Mole Concept", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_11, 1, 5,
            listOf("Mole & Molar Mass", "Stoichiometry & Limiting Reagent", "Concentration Terms (M, m, N)", "Eudiometry & Equivalent Concept")
        ),
        SeedChapterSpec(
            "chem_phy_02", "Atomic Structure", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_11, 2, 4,
            listOf("Bohr Model & Hydrogen Spectrum", "Dual Nature & Heisenberg Uncertainty", "Quantum Numbers & Orbitals", "Radial & Angular Nodes")
        ),
        SeedChapterSpec(
            "chem_phy_03", "Gaseous State", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_11, 3, 3,
            listOf("Gas Laws & Dalton's Law", "Graham's Law of Diffusion", "Kinetic Theory & Maxwell Distribution", "Real Gases & Van der Waals Equation")
        ),
        SeedChapterSpec(
            "chem_phy_04", "Thermodynamics", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_11, 4, 5,
            listOf("First Law & Enthalpy", "Thermochemistry & Hess's Law", "Entropy & Second Law", "Gibbs Free Energy & Spontaneity")
        ),
        SeedChapterSpec(
            "chem_phy_05", "Chemical Equilibrium", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_11, 5, 4,
            listOf("Kp, Kc & Reaction Quotient", "Degree of Dissociation & Vapour Density", "Le Chatelier's Principle")
        ),
        SeedChapterSpec(
            "chem_phy_06", "Ionic Equilibrium", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_11, 6, 5,
            listOf("Ostwald Dilution Law & pH", "Buffer Solutions & Henderson Equation", "Salt Hydrolysis", "Acid-Base Titrations", "Solubility Product (Ksp)")
        ),
        SeedChapterSpec(
            "chem_phy_07", "Redox Reactions", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_11, 7, 3,
            listOf("Oxidation Number & Disproportionation", "Balancing Redox Equations", "n-Factor & Volumetric Titrations")
        ),
        SeedChapterSpec(
            "chem_phy_08", "Solutions", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_12, 8, 4,
            listOf("Raoult's Law & Ideal/Non-Ideal", "Colligative Properties", "Van't Hoff Factor")
        ),
        SeedChapterSpec(
            "chem_phy_09", "Electrochemistry", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_12, 9, 5,
            listOf("Galvanic Cells & Nernst Equation", "Conductance & Kohlrausch Law", "Electrolysis & Faraday's Laws")
        ),
        SeedChapterSpec(
            "chem_phy_10", "Chemical Kinetics", Subject.CHEMISTRY, ChemistrySection.PHYSICAL, ClassLevel.CLASS_12, 10, 4,
            listOf("Rate Law & Order of Reaction", "Zero & First Order Integrated Equations", "Arrhenius Equation & Catalysis", "Parallel & Sequential Reactions")
        ),

        // ================= CHEMISTRY: INORGANIC =================
        SeedChapterSpec(
            "chem_ino_01", "Periodic Classification", Subject.CHEMISTRY, ChemistrySection.INORGANIC, ClassLevel.CLASS_11, 11, 4,
            listOf("Modern Periodic Table", "Atomic & Ionic Radii", "Ionisation Enthalpy Exceptions", "Electron Gain Enthalpy & Electronegativity")
        ),
        SeedChapterSpec(
            "chem_ino_02", "Chemical Bonding", Subject.CHEMISTRY, ChemistrySection.INORGANIC, ClassLevel.CLASS_11, 12, 5,
            listOf("Lewis Structures & Formal Charge", "VSEPR Theory & Shapes", "Hybridisation & Bent's Rule", "Molecular Orbital Theory (MOT)", "Dipole Moment & Fajan's Rules", "Hydrogen Bonding")
        ),
        SeedChapterSpec(
            "chem_ino_03", "Coordination Compounds", Subject.CHEMISTRY, ChemistrySection.INORGANIC, ClassLevel.CLASS_12, 13, 5,
            listOf("Werner's Theory & IUPAC Nomenclature", "Structural & Stereoisomerism", "Valence Bond Theory", "Crystal Field Theory (CFT) & Colour", "Metal Carbonyls & Synergic Bonding")
        ),
        SeedChapterSpec(
            "chem_ino_04", "p-Block Elements", Subject.CHEMISTRY, ChemistrySection.INORGANIC, ClassLevel.CLASS_12, 14, 4,
            listOf("Group 13-14 Trends & Oxyacids", "Group 15-16 Hydrides & Oxides", "Halogens & Interhalogen Compounds", "Noble Gases & Xenon Compounds")
        ),
        SeedChapterSpec(
            "chem_ino_05", "d- and f-Block Elements", Subject.CHEMISTRY, ChemistrySection.INORGANIC, ClassLevel.CLASS_12, 15, 4,
            listOf("Transition Metal Properties & Oxidation States", "KMnO4 & K2Cr2O7 Reactions", "Lanthanoid Contraction & Actinoids")
        ),
        SeedChapterSpec(
            "chem_ino_06", "Salt Analysis & Metallurgy", Subject.CHEMISTRY, ChemistrySection.INORGANIC, ClassLevel.CLASS_12, 16, 3,
            listOf("Qualitative Cation Groups", "Anion Identification Tests", "Ellingham Diagrams & Extraction")
        ),

        // ================= CHEMISTRY: ORGANIC =================
        SeedChapterSpec(
            "chem_org_01", "General Organic Chemistry", Subject.CHEMISTRY, ChemistrySection.ORGANIC, ClassLevel.CLASS_11, 17, 5,
            listOf("IUPAC Nomenclature", "Inductive, Resonance & Hyperconjugation", "Aromaticity & Anti-Aromaticity", "Stability of Carbocations, Carbanions & Radicals", "Acidic & Basic Strength Comparison")
        ),
        SeedChapterSpec(
            "chem_org_02", "Isomerism", Subject.CHEMISTRY, ChemistrySection.ORGANIC, ClassLevel.CLASS_11, 18, 5,
            listOf("Structural Isomerism", "Geometrical (E-Z) Isomerism", "Optical Activity & R/S Configuration", "Conformational Analysis (Ethane, Butane, Cyclohexane)")
        ),
        SeedChapterSpec(
            "chem_org_03", "Hydrocarbons", Subject.CHEMISTRY, ChemistrySection.ORGANIC, ClassLevel.CLASS_11, 19, 5,
            listOf("Alkanes & Free Radical Halogenation", "Alkenes: Electrophilic Addition & Ozonolysis", "Alkynes: Acidity & Additions", "Benzene & Electrophilic Aromatic Substitution")
        ),
        SeedChapterSpec(
            "chem_org_04", "Haloalkanes and Haloarenes", Subject.CHEMISTRY, ChemistrySection.ORGANIC, ClassLevel.CLASS_12, 20, 4,
            listOf("SN1 vs SN2 Mechanisms", "E1 vs E2 Elimination", "Aryl Halides & Benzyne Mechanism")
        ),
        SeedChapterSpec(
            "chem_org_05", "Alcohols, Phenols and Ethers", Subject.CHEMISTRY, ChemistrySection.ORGANIC, ClassLevel.CLASS_12, 21, 4,
            listOf("Preparation & Lucas Test", "Pinacol-Pinacolone Rearrangement", "Phenol: Reimer-Tiemann & Kolbe Reactions", "Ethers & Williamson Synthesis")
        ),
        SeedChapterSpec(
            "chem_org_06", "Aldehydes and Ketones", Subject.CHEMISTRY, ChemistrySection.ORGANIC, ClassLevel.CLASS_12, 22, 5,
            listOf("Nucleophilic Addition & Grignard", "Aldol Condensation", "Cannizzaro & Haloform Reactions", "Wittig, Clemmensen & Wolff-Kishner")
        ),
        SeedChapterSpec(
            "chem_org_07", "Carboxylic Acids", Subject.CHEMISTRY, ChemistrySection.ORGANIC, ClassLevel.CLASS_12, 23, 4,
            listOf("Acidity Trends", "Esterification & Decarboxylation", "HVZ Reaction & Acid Derivatives")
        ),
        SeedChapterSpec(
            "chem_org_08", "Amines", Subject.CHEMISTRY, ChemistrySection.ORGANIC, ClassLevel.CLASS_12, 24, 4,
            listOf("Basicity in Gas vs Aqueous Phase", "Gabriel Phthalimide & Hoffmann Bromamide", "Diazonium Salts & Coupling Reactions")
        ),
        SeedChapterSpec(
            "chem_org_09", "Biomolecules", Subject.CHEMISTRY, ChemistrySection.ORGANIC, ClassLevel.CLASS_12, 25, 3,
            listOf("Carbohydrates & Mutarotation", "Amino Acids & Zwitterion", "Proteins & Nucleic Acids")
        ),

        // ================= MATHEMATICS (CLASS 11) =================
        SeedChapterSpec(
            "math_11_01", "Basic Mathematics", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 1, 4,
            listOf("Wavy Curve Method & Inequalities", "Modulus Equations & Inequalities", "Logarithms & Exponential Equations")
        ),
        SeedChapterSpec(
            "math_11_02", "Sets and Relations", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 2, 3,
            listOf("Set Operations & Venn Diagrams", "Equivalence Relations (Reflexive, Symmetric, Transitive)", "Counting Relations")
        ),
        SeedChapterSpec(
            "math_11_03", "Functions", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 3, 5,
            listOf("Domain & Range", "One-One, Onto & Bijective Functions", "Inverse & Composite Functions", "Periodic, Even-Odd & Functional Equations")
        ),
        SeedChapterSpec(
            "math_11_04", "Trigonometry", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 4, 4,
            listOf("Compound & Multiple Angle Identities", "Trigonometric Equations & General Solutions", "Properties of Triangles & Inverse Trig")
        ),
        SeedChapterSpec(
            "math_11_05", "Quadratic Equations", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 5, 5,
            listOf("Nature of Roots & Symmetric Expressions", "Common Roots Condition", "Graph of Quadratic & Range", "Location of Roots")
        ),
        SeedChapterSpec(
            "math_11_06", "Complex Numbers", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 6, 4,
            listOf("Algebra, Modulus & Argument", "Euler Form & De Moivre's Theorem", "Cube & nth Roots of Unity", "Geometry & Rotation in Argand Plane")
        ),
        SeedChapterSpec(
            "math_11_07", "Sequence and Series", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 7, 5,
            listOf("AP, GP & AGP", "AM-GM-HM Inequalities", "Method of Differences & Telescoping Sums", "Special Sigma Series")
        ),
        SeedChapterSpec(
            "math_11_08", "Permutations and Combinations", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 8, 5,
            listOf("Fundamental Principle & Gap/Bundle Method", "Circular Permutations", "Multinomial Theorem & Distribution of Objects", "Derangements & Geometrical Counting")
        ),
        SeedChapterSpec(
            "math_11_09", "Binomial Theorem", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 9, 4,
            listOf("General & Middle Term", "Properties of Binomial Coefficients", "Multinomial Expansion & Remainder Problems")
        ),
        SeedChapterSpec(
            "math_11_10", "Straight Lines", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 10, 4,
            listOf("Forms of Line & Angle Between Lines", "Centres of Triangle", "Distance, Foot of Perpendicular & Image", "Family of Lines & Pair of Lines")
        ),
        SeedChapterSpec(
            "math_11_11", "Circles", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 11, 4,
            listOf("Standard Equation & Parametric Form", "Tangent, Normal & Chord of Contact", "Radical Axis & Coaxial Circles", "Orthogonality & Common Tangents")
        ),
        SeedChapterSpec(
            "math_11_12", "Conic Sections", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_11, 12, 5,
            listOf("Parabola: Focal Properties & Tangents", "Ellipse: Eccentricity & Auxiliary Circle", "Hyperbola & Rectangular Hyperbola", "Common Tangents & Reflection Properties")
        ),

        // ================= MATHEMATICS (CLASS 12) =================
        SeedChapterSpec(
            "math_12_01", "Probability", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 13, 4,
            listOf("Conditional Probability", "Total Probability & Bayes' Theorem", "Bernoulli Trials & Distributions")
        ),
        SeedChapterSpec(
            "math_12_02", "Matrices and Determinants", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 14, 5,
            listOf("Properties of Determinants", "Adjoint, Inverse & Cayley-Hamilton", "System of Linear Equations")
        ),
        SeedChapterSpec(
            "math_12_03", "Limits", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 15, 4,
            listOf("Indeterminate Forms & Standard Limits", "Series Expansion & L'Hopital Rule", "Sandwich Theorem")
        ),
        SeedChapterSpec(
            "math_12_04", "Continuity and Differentiability", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 16, 4,
            listOf("Types of Discontinuity", "LHD, RHD & Sharp Corners", "Functional Equation Differentiation")
        ),
        SeedChapterSpec(
            "math_12_05", "Applications of Derivatives", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 17, 5,
            listOf("Tangent & Normal", "Monotonicity & Inequalities", "Maxima & Minima", "Rolle's & Lagrange Mean Value Theorems")
        ),
        SeedChapterSpec(
            "math_12_06", "Indefinite and Definite Integration", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 18, 5,
            listOf("Substitution & Integration by Parts", "King's & Queen's Properties", "Leibnitz Rule & Limit of a Sum", "Area Under Curves")
        ),
        SeedChapterSpec(
            "math_12_07", "Differential Equations", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 19, 4,
            listOf("Order, Degree & Variable Separable", "Homogeneous & Linear DE (Integrating Factor)", "Exact Forms & Orthogonal Trajectories")
        ),
        SeedChapterSpec(
            "math_12_08", "Vectors", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 20, 4,
            listOf("Linear Combination & Collinearity", "Scalar & Vector Triple Products", "Vector Equations")
        ),
        SeedChapterSpec(
            "math_12_09", "Three-Dimensional Geometry", Subject.MATHEMATICS, ChemistrySection.NONE, ClassLevel.CLASS_12, 21, 5,
            listOf("Direction Cosines & Lines in 3D", "Skew Lines & Shortest Distance", "Planes, Foot & Image of a Point")
        )
    )

    fun createSeedChapters(userId: String): List<Chapter> =
        defaultSpecs.map { spec ->
            Chapter(
                id = spec.id,
                userId = userId,
                name = spec.name,
                subject = spec.subject.name,
                section = spec.section.firestoreValue,
                classLevel = spec.classLevel.name,
                orderIndex = spec.orderIndex.toLong(),
                syllabusVersion = DEFAULT_SYLLABUS_VERSION,
                importance = spec.importance.toLong()
            )
        }

    fun createSeedTopics(userId: String): List<Topic> =
        defaultSpecs.flatMap { spec ->
            spec.topics.mapIndexed { idx, topicName ->
                Topic(
                    id = "${spec.id}_t_${idx + 1}",
                    userId = userId,
                    chapterId = spec.id,
                    subject = spec.subject.name,
                    name = topicName,
                    orderIndex = (idx + 1).toLong()
                )
            }
        }
}
