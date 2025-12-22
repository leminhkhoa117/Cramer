/**
 * ABTS Topic Templates - Pre-defined topics with facts for AI generation.
 * 
 * Each template contains:
 * - id: Unique identifier
 * - name: Display name
 * - hashtags: Tags for categorization
 * - facts: 15-25 verified facts for AI to use in generation
 * 
 * @since 2025-12-20 - ABTS v2.0
 */

/**
 * Topic categories with emoji and names.
 */
export const TOPIC_CATEGORIES = [
    {
        id: "environment",
        emoji: "🌍",
        name: "Environment",
        name_vi: "Môi trường",
        description: "Climate change, renewable energy, conservation"
    },
    {
        id: "technology",
        emoji: "💻",
        name: "Technology",
        name_vi: "Công nghệ",
        description: "AI, internet, digital transformation"
    },
    {
        id: "education",
        emoji: "📚",
        name: "Education",
        name_vi: "Giáo dục",
        description: "Online learning, university, childhood education"
    },
    {
        id: "health",
        emoji: "🏥",
        name: "Health & Medicine",
        name_vi: "Y tế",
        description: "Healthcare, mental health, nutrition"
    },
    {
        id: "society",
        emoji: "👥",
        name: "Society",
        name_vi: "Xã hội",
        description: "Urban development, demographics, social issues"
    },
    {
        id: "business",
        emoji: "💼",
        name: "Business & Economy",
        name_vi: "Kinh doanh",
        description: "Globalization, entrepreneurship, markets"
    },
    {
        id: "science",
        emoji: "🔬",
        name: "Science",
        name_vi: "Khoa học",
        description: "Space, biology, physics, research"
    },
    {
        id: "history",
        emoji: "🏛️",
        name: "History & Archaeology",
        name_vi: "Lịch sử",
        description: "Ancient civilizations, historical discoveries"
    },
    {
        id: "arts",
        emoji: "🎨",
        name: "Arts & Culture",
        name_vi: "Nghệ thuật",
        description: "Music, literature, painting, architecture"
    },
    {
        id: "travel",
        emoji: "✈️",
        name: "Travel & Tourism",
        name_vi: "Du lịch",
        description: "Destinations, cultural tourism, travel industry"
    },
    // Listening-specific categories (Phase 3)
    {
        id: "listening_part1",
        emoji: "🎧",
        name: "Listening Part 1",
        name_vi: "Nghe Part 1",
        description: "Everyday social conversations (booking, inquiry)",
        skill: "LISTENING"
    },
    {
        id: "listening_part2",
        emoji: "🗣️",
        name: "Listening Part 2",
        name_vi: "Nghe Part 2",
        description: "Social monologues (tours, orientations)",
        skill: "LISTENING"
    },
    {
        id: "listening_part3",
        emoji: "👥",
        name: "Listening Part 3",
        name_vi: "Nghe Part 3",
        description: "Academic discussions (tutorials, projects)",
        skill: "LISTENING"
    },
    {
        id: "listening_part4",
        emoji: "🎓",
        name: "Listening Part 4",
        name_vi: "Nghe Part 4",
        description: "Academic lectures (university, research)",
        skill: "LISTENING"
    },
    // Writing-specific categories (Phase 4)
    {
        id: "writing_task1_chart",
        emoji: "📊",
        name: "Writing Task 1: Charts",
        name_vi: "Viết Task 1: Biểu đồ",
        description: "Bar, line, pie charts and tables",
        skill: "WRITING"
    },
    {
        id: "writing_task1_process",
        emoji: "🔄",
        name: "Writing Task 1: Process",
        name_vi: "Viết Task 1: Quy trình",
        description: "Process diagrams and maps",
        skill: "WRITING"
    },
    {
        id: "writing_task1_letter",
        emoji: "✉️",
        name: "Writing Task 1: Letters",
        name_vi: "Viết Task 1: Thư",
        description: "General Training letters (complaint, request, etc.)",
        skill: "WRITING",
        testType: "GENERAL_TRAINING"
    },
    {
        id: "writing_task2_opinion",
        emoji: "💭",
        name: "Writing Task 2: Opinion",
        name_vi: "Viết Task 2: Quan điểm",
        description: "Agree/disagree and opinion essays",
        skill: "WRITING"
    },
    {
        id: "writing_task2_discussion",
        emoji: "⚖️",
        name: "Writing Task 2: Discussion",
        name_vi: "Viết Task 2: Thảo luận",
        description: "Discuss both views and advantages/disadvantages",
        skill: "WRITING"
    },
    {
        id: "writing_task2_problem",
        emoji: "🔧",
        name: "Writing Task 2: Problem/Solution",
        name_vi: "Viết Task 2: Vấn đề/Giải pháp",
        description: "Causes, problems and solutions essays",
        skill: "WRITING"
    }
];

/**
 * Sample topic templates organized by category.
 */
export const SAMPLE_TOPIC_TEMPLATES = {
    environment: [
        {
            id: "solar_energy",
            name: "Solar Energy Development",
            hashtags: ["renewable", "technology", "sustainability"],
            facts: [
                "Solar panels convert sunlight into electricity using photovoltaic cells made primarily from silicon.",
                "The first practical solar cell was invented at Bell Laboratories in 1954 with 6% efficiency.",
                "Modern commercial solar panels achieve efficiencies between 15-22%.",
                "China produces over 70% of the world's solar panels as of 2023.",
                "Solar energy is the fastest-growing source of renewable electricity globally.",
                "The cost of solar panels has decreased by 99% since 1977.",
                "Solar farms can generate electricity for 25-30 years with minimal maintenance.",
                "Floating solar panels (floatovoltaics) are installed on water bodies to save land.",
                "Solar energy production varies significantly based on geographic location and weather.",
                "Germany was the first country to exceed 50% renewable electricity in 2020.",
                "Solar thermal technology uses mirrors to concentrate sunlight for power generation.",
                "Rooftop solar installations can reduce household electricity bills by 50-90%.",
                "The Mojave Desert in the US hosts some of the largest solar power plants.",
                "Solar panel recycling is an emerging industry addressing end-of-life disposal.",
                "Battery storage technology is crucial for managing solar energy's intermittency.",
                "India aims to achieve 500 GW of renewable energy capacity by 2030."
            ]
        },
        {
            id: "ocean_pollution",
            name: "Ocean Plastic Pollution",
            hashtags: ["marine", "pollution", "conservation"],
            facts: [
                "Approximately 8 million metric tons of plastic enter the ocean each year.",
                "The Great Pacific Garbage Patch is twice the size of Texas.",
                "Microplastics have been found in 90% of bottled water brands tested.",
                "Plastic pollution affects over 700 marine species worldwide.",
                "Single-use plastics account for 50% of all plastic produced annually.",
                "It takes 400-1000 years for most plastics to decompose in the environment.",
                "Ocean plastic is projected to triple by 2040 without significant intervention.",
                "Less than 10% of global plastic waste is actually recycled.",
                "Marine animals often mistake plastic for food, leading to starvation.",
                "The fishing industry contributes 10% of ocean plastic through discarded nets.",
                "Plastic production is expected to double within the next 20 years.",
                "Some countries have banned single-use plastic bags and straws.",
                "The Ocean Cleanup project aims to remove plastic from ocean gyres.",
                "Microplastics have been found in human blood and lung tissue.",
                "Biodegradable plastics often require specific conditions to break down."
            ]
        }
    ],

    technology: [
        {
            id: "artificial_intelligence",
            name: "Artificial Intelligence Development",
            hashtags: ["AI", "technology", "future"],
            facts: [
                "The term 'Artificial Intelligence' was coined by John McCarthy in 1956.",
                "Machine learning is a subset of AI that enables computers to learn from data.",
                "Deep learning uses neural networks with many layers to process complex patterns.",
                "GPT-4, released in 2023, can process both text and images.",
                "AI systems have surpassed humans in specific tasks like chess and Go.",
                "Natural language processing enables AI to understand human language.",
                "Self-driving cars use AI for navigation, object detection, and decision-making.",
                "AI-powered diagnostic tools can detect certain cancers with 94% accuracy.",
                "The global AI market is expected to reach $1.8 trillion by 2030.",
                "AI raises ethical concerns about job displacement and privacy.",
                "Generative AI can create realistic images, music, and text.",
                "AI assistants like Siri and Alexa use voice recognition technology.",
                "Facial recognition technology uses AI to identify individuals.",
                "AI algorithms power recommendation systems on Netflix and YouTube.",
                "Quantum computing may significantly accelerate AI development."
            ]
        }
    ],

    education: [
        {
            id: "online_learning",
            name: "The Rise of Online Learning",
            hashtags: ["education", "technology", "pandemic"],
            facts: [
                "Global online education market reached $400 billion in 2023.",
                "MOOCs (Massive Open Online Courses) were introduced in 2008.",
                "Coursera has over 100 million registered learners worldwide.",
                "COVID-19 pandemic pushed over 1.5 billion students to remote learning.",
                "Online learning can be 25-60% faster than traditional classroom learning.",
                "Students retain 25-60% more information with e-learning compared to 8-10% in classroom.",
                "Khan Academy provides free education to over 20 million monthly users.",
                "Virtual reality is being used to create immersive learning experiences.",
                "Adult learners make up the majority of online course participants.",
                "Microlearning delivers content in small, focused segments of 5-10 minutes.",
                "Gamification in education increases engagement by up to 60%.",
                "AI tutoring systems can personalize learning paths for individual students.",
                "Language learning apps like Duolingo have over 500 million downloads.",
                "Asynchronous learning allows students to access content at their own pace.",
                "Digital credentials and certificates are becoming increasingly recognized by employers."
            ]
        }
    ],

    health: [
        {
            id: "mental_health",
            name: "Mental Health in Modern Society",
            hashtags: ["health", "psychology", "wellbeing"],
            facts: [
                "One in four people globally will experience a mental health condition.",
                "Depression affects over 280 million people worldwide.",
                "Anxiety disorders are the most common mental health conditions.",
                "Mental health conditions cost the global economy $1 trillion annually.",
                "Only 50% of people with depression in developed countries receive treatment.",
                "Social media use is linked to increased rates of anxiety and depression in teens.",
                "Mindfulness meditation has been shown to reduce symptoms of anxiety.",
                "Exercise can be as effective as medication for mild to moderate depression.",
                "Sleep deprivation significantly impacts mental health and cognitive function.",
                "The COVID-19 pandemic increased global prevalence of anxiety by 25%.",
                "Workplace stress accounts for $300 billion in healthcare costs in the US.",
                "Therapy delivered via video conferencing can be as effective as in-person sessions.",
                "Mental health stigma prevents many people from seeking help.",
                "Early intervention in adolescence can prevent chronic mental health conditions.",
                "The global shortage of mental health professionals exceeds 13 million."
            ]
        }
    ],

    // Listening-specific scenarios (Phase 3)
    listening_part1: [
        {
            id: "hostel_booking",
            name: "Hostel Accommodation Booking",
            hashtags: ["accommodation", "travel", "booking"],
            skill: "LISTENING",
            partNumber: 1,
            facts: [
                "The hostel is located at 47 Riverside Drive, near the central train station.",
                "Room types available include single rooms ($45/night) and shared dorms ($25/night).",
                "Check-in time is 2:00 PM and check-out is by 11:00 AM.",
                "The hostel offers free Wi-Fi and a shared kitchen on each floor.",
                "Breakfast is available for an additional $8 per person.",
                "The hostel has 24-hour reception and security cameras.",
                "Guests must provide a valid ID and credit card at check-in.",
                "Cancellation is free up to 48 hours before arrival.",
                "The nearest bus stop is a 5-minute walk on Miller Street.",
                "Special dietary requirements can be accommodated with advance notice.",
                "Parking is available for $12 per day in the underground garage.",
                "The hostel organizes free walking tours every Saturday at 10 AM.",
                "Laundry facilities are available for $5 per load.",
                "Maximum stay is 14 consecutive nights during peak season."
            ]
        },
        {
            id: "gym_membership",
            name: "Gym Membership Inquiry",
            hashtags: ["fitness", "membership", "health"],
            skill: "LISTENING",
            partNumber: 1,
            facts: [
                "The fitness center is open from 6 AM to 10 PM on weekdays.",
                "Annual membership costs $599 with a $50 joining fee.",
                "Monthly membership is available at $65 without any contract.",
                "Student discounts of 20% are available with valid student ID.",
                "The gym has a swimming pool, sauna, and steam room.",
                "Personal training sessions cost $75 for one hour.",
                "Group fitness classes include yoga, spinning, and HIIT.",
                "Free trial week is offered to first-time visitors.",
                "Lockers are provided free of charge; bring your own padlock.",
                "The gym has branches at 3 locations in the city.",
                "Off-peak hours (10 AM - 4 PM) membership is 30% cheaper.",
                "Members can freeze their membership for up to 3 months per year.",
                "Family packages offer 25% discount for households.",
                "The app allows members to book classes up to 7 days in advance."
            ]
        }
    ],

    listening_part2: [
        {
            id: "museum_tour",
            name: "Natural History Museum Tour",
            hashtags: ["museum", "tour", "education"],
            skill: "LISTENING",
            partNumber: 2,
            facts: [
                "The Natural History Museum was established in 1872.",
                "The museum has over 80 million specimens in its collection.",
                "The main entrance hall features a 25-meter blue whale skeleton.",
                "The dinosaur gallery is the most popular exhibit, attracting 2 million visitors annually.",
                "The museum is free to enter but special exhibitions require tickets.",
                "Guided tours run every hour from 10 AM to 4 PM.",
                "The building was designed by architect Alfred Waterhouse.",
                "The museum's library contains over 1 million books and journals.",
                "Interactive exhibits allow visitors to touch real fossils.",
                "The café is located on the ground floor near the east wing.",
                "Gift shops sell educational toys and replica fossils.",
                "The museum hosts overnight experiences for children aged 7-12.",
                "Wheelchair access is available at all entrances.",
                "Photography is allowed but flash photography is prohibited.",
                "Audio guides are available in 10 languages for $5 rental."
            ]
        },
        {
            id: "campus_orientation",
            name: "University Campus Orientation",
            hashtags: ["university", "campus", "students"],
            skill: "LISTENING",
            partNumber: 2,
            facts: [
                "The main library is open 24 hours during exam periods.",
                "Student ID cards can be collected from the administration building.",
                "The sports center offers free access to all current students.",
                "Meal plans start at $2,500 per semester with 3 meals daily.",
                "Campus shuttle buses run every 15 minutes between buildings.",
                "The engineering building houses the largest computer labs.",
                "Health services are located next to the student union.",
                "International students must attend visa information sessions.",
                "Career counseling is available by appointment on Thursdays.",
                "The campus has 12 residential halls accommodating 5,000 students.",
                "Wi-Fi is available throughout the campus using student credentials.",
                "The bookstore offers 15% discount on textbooks for early purchases.",
                "Bike rental is available for $50 per semester.",
                "The student union building houses cafeterias and recreation areas."
            ]
        }
    ],

    listening_part3: [
        {
            id: "research_project",
            name: "Research Project Discussion",
            hashtags: ["academic", "research", "study"],
            skill: "LISTENING",
            partNumber: 3,
            facts: [
                "The research project focuses on consumer behavior in online shopping.",
                "Data collection will use both surveys and interviews.",
                "The sample size should be at least 200 participants for statistical validity.",
                "The deadline for the literature review is in three weeks.",
                "Previous studies have shown conflicting results on impulse buying online.",
                "The research methodology must be approved by the ethics committee.",
                "Both qualitative and quantitative approaches will be combined.",
                "The final presentation is scheduled for December 15th.",
                "Each team member must contribute to at least two sections.",
                "References must follow APA 7th edition format.",
                "The supervisor is available for consultations on Wednesdays.",
                "Primary data collection should begin by mid-semester.",
                "Similar studies have used convenience sampling methods.",
                "The research paper should be between 8,000-10,000 words."
            ]
        }
    ],

    listening_part4: [
        {
            id: "climate_lecture",
            name: "Climate Science Lecture",
            hashtags: ["climate", "science", "environment"],
            skill: "LISTENING",
            partNumber: 4,
            facts: [
                "Global average temperature has risen 1.1°C since pre-industrial times.",
                "The Paris Agreement aims to limit warming to 1.5°C above pre-industrial levels.",
                "Greenhouse gases include carbon dioxide, methane, and nitrous oxide.",
                "The Arctic is warming twice as fast as the global average.",
                "Sea levels have risen approximately 20 cm since 1900.",
                "Coral reefs could decline by 70-90% at 1.5°C warming.",
                "Renewable energy sources produced 29% of global electricity in 2023.",
                "Carbon capture technology is being developed to remove CO2 from the atmosphere.",
                "Climate models predict more frequent extreme weather events.",
                "The permafrost contains twice as much carbon as the atmosphere.",
                "Deforestation accounts for approximately 10% of global emissions.",
                "Electric vehicles could reduce transport emissions by 50%.",
                "Methane is 80 times more potent than CO2 over 20 years.",
                "Climate adaptation strategies include flood defenses and drought-resistant crops.",
                "The transition to net-zero emissions requires $4 trillion annual investment."
            ]
        },
        {
            id: "archaeology_lecture",
            name: "Archaeological Discovery Lecture",
            hashtags: ["archaeology", "history", "discovery"],
            skill: "LISTENING",
            partNumber: 4,
            facts: [
                "The excavation site was discovered in 2018 by farmers during construction.",
                "Archaeological dating methods include radiocarbon and thermoluminescence.",
                "The settlement dates back approximately 3,500 years to the Bronze Age.",
                "Over 12,000 artifacts have been recovered from the site.",
                "DNA analysis revealed the inhabitants originated from Central Asia.",
                "The discovery includes the largest Bronze Age pottery collection in the region.",
                "Ground-penetrating radar identified 15 previously unknown structures.",
                "The burial site contained jewelry made from gold and lapis lazuli.",
                "Evidence suggests the settlement engaged in long-distance trade.",
                "The excavation employs 40 archaeologists and 200 local workers.",
                "Funding comes from three universities and the national government.",
                "The site will become a protected UNESCO heritage location.",
                "Researchers have published 12 peer-reviewed papers on findings.",
                "A museum will be built near the site to display artifacts by 2027."
            ]
        }
    ],

    // ==================== WRITING TEMPLATES (Phase 4) ====================

    writing_task1_chart: [
        {
            id: "electricity_sources",
            name: "Electricity Generation by Source",
            hashtags: ["energy", "environment", "statistics"],
            chartType: "bar_grouped",
            facts: [
                "Global electricity generation reached 29,165 TWh in 2022.",
                "Coal accounted for 36% of global electricity in 2022, down from 41% in 2010.",
                "Renewables generated 30% of global electricity in 2022.",
                "Natural gas provided 22% of world electricity in 2022.",
                "Nuclear power contributed 9% of global electricity in 2022.",
                "Solar power grew from 0.05% in 2010 to 4.5% in 2022.",
                "Wind energy increased from 1.6% to 7.6% between 2010 and 2022.",
                "Hydropower remained stable at approximately 15% over the decade.",
                "China produces 31% of the world's electricity.",
                "The US generates 16% of global electricity.",
                "The EU produces 11% of world electricity.",
                "India's electricity production has doubled since 2010."
            ]
        },
        {
            id: "household_spending",
            name: "Household Expenditure Categories",
            hashtags: ["economics", "consumer", "statistics"],
            chartType: "pie_standard",
            facts: [
                "Average UK household spends £2,600 monthly on living costs.",
                "Housing costs (rent/mortgage) account for 32% of household budgets.",
                "Food and groceries represent 15% of household spending.",
                "Transport costs average 13% of household income.",
                "Utilities (electricity, gas, water) take up 7% of budgets.",
                "Recreation and entertainment account for 9% of spending.",
                "Healthcare and personal care represent 4% of budgets.",
                "Clothing and footwear average 3% of household expenses.",
                "Education costs vary from 2-8% depending on family composition.",
                "Savings rates average 6% of income in developed countries.",
                "Restaurant and takeaway spending has increased 25% since 2015.",
                "Online shopping now accounts for 30% of non-food purchases."
            ]
        }
    ],

    writing_task1_process: [
        {
            id: "water_treatment",
            name: "Water Treatment Process",
            hashtags: ["environment", "engineering", "process"],
            chartType: "process",
            facts: [
                "Water treatment involves multiple stages of purification.",
                "Raw water first passes through screens to remove large debris.",
                "Coagulation uses chemicals to bind small particles together.",
                "Flocculation gently mixes water to form larger particle clumps.",
                "Sedimentation allows heavy particles to settle to the bottom.",
                "Filtration removes remaining particles through sand and gravel.",
                "Disinfection kills bacteria using chlorine or UV light.",
                "pH adjustment ensures water is not too acidic or alkaline.",
                "Fluoride may be added to help prevent tooth decay.",
                "Storage tanks hold treated water before distribution.",
                "The entire process takes 2-4 hours from start to finish.",
                "Quality testing occurs at multiple stages of treatment."
            ]
        }
    ],

    writing_task2_opinion: [
        {
            id: "remote_work",
            name: "Remote Work and Productivity",
            hashtags: ["work", "technology", "society"],
            essayType: "opinion",
            facts: [
                "75% of workers say they are equally or more productive at home.",
                "Remote workers save an average of 40 minutes daily on commuting.",
                "Office occupancy rates remain at 50% in major cities post-pandemic.",
                "29% of workers report increased stress from work-life boundary blur.",
                "Companies report 15-25% savings on office space costs.",
                "Remote workers take 10% fewer sick days on average.",
                "62% of employees prefer hybrid work arrangements.",
                "Virtual meetings have increased by 300% since 2020.",
                "Collaboration software market grew to $45 billion in 2023.",
                "36% of jobs can be performed entirely remotely.",
                "Remote work has reduced carbon emissions by 54 million tons annually.",
                "34% of remote workers report feeling isolated."
            ]
        },
        {
            id: "social_media_youth",
            name: "Social Media's Impact on Young People",
            hashtags: ["technology", "health", "education"],
            essayType: "opinion",
            facts: [
                "Average teenager spends 4.8 hours daily on social media.",
                "95% of teens aged 13-17 have access to a smartphone.",
                "Social media use correlates with 25% higher rates of anxiety in teens.",
                "70% of teens check social media multiple times per hour.",
                "Cyberbullying affects 37% of students aged 12-17.",
                "Social media platforms have 4.9 billion users worldwide.",
                "TikTok users spend an average of 95 minutes per day on the app.",
                "68% of parents worry about screen time impact on children.",
                "Social media can increase political awareness among youth.",
                "Online communities provide support for marginalized groups.",
                "Schools report decreased attention spans among students.",
                "Some countries have introduced social media age restrictions."
            ]
        }
    ],

    writing_task2_discussion: [
        {
            id: "university_education",
            name: "Value of University Education",
            hashtags: ["education", "career", "economics"],
            essayType: "discussion",
            facts: [
                "University graduates earn 57% more than non-graduates on average.",
                "Student debt in the US totals $1.77 trillion.",
                "35% of jobs require a bachelor's degree or higher.",
                "Alternative credentials have grown 300% since 2015.",
                "The average cost of a 4-year degree is $35,000 per year.",
                "Graduate unemployment rate is 4.3% vs 7.6% for non-graduates.",
                "70% of employers value experience over qualifications.",
                "Online courses have made education more accessible globally.",
                "Vocational training leads to employment rates of 85%.",
                "Return on investment for degrees varies significantly by field.",
                "Liberal arts graduates have 25% higher career satisfaction.",
                "STEM graduates have the highest starting salaries on average."
            ]
        }
    ],

    writing_task2_problem: [
        {
            id: "plastic_pollution",
            name: "Plastic Pollution Crisis",
            hashtags: ["environment", "policy", "health"],
            essayType: "problem_solution",
            facts: [
                "8 million tons of plastic enter oceans annually.",
                "Only 9% of plastic ever produced has been recycled.",
                "By 2050, oceans could contain more plastic than fish by weight.",
                "Microplastics have been found in 80% of tap water samples.",
                "Single-use plastics account for 50% of all plastic waste.",
                "Plastic takes 400-1000 years to decompose naturally.",
                "130 countries have introduced plastic bag regulations.",
                "The recycling industry is worth $200 billion globally.",
                "Biodegradable alternatives cost 2-3 times more than plastic.",
                "Marine animals ingest an average of 40 pieces of plastic.",
                "Extended Producer Responsibility laws are increasing globally.",
                "Consumer awareness of plastic issues has doubled since 2018."
            ]
        },
        {
            id: "urban_traffic",
            name: "Urban Traffic Congestion",
            hashtags: ["transport", "urban", "environment"],
            essayType: "problem_solution",
            facts: [
                "Traffic congestion costs the US economy $87 billion annually.",
                "Average commuter spends 54 hours per year stuck in traffic.",
                "Road transport accounts for 16% of global CO2 emissions.",
                "Public transport usage has declined 25% since 2019.",
                "Congestion pricing has reduced traffic by 30% in Singapore.",
                "Electric vehicles now represent 14% of new car sales.",
                "Cycling infrastructure investment has increased 200% in Europe.",
                "Remote work has reduced peak-hour traffic by 15%.",
                "Smart traffic light systems can improve flow by 25%.",
                "Car sharing services have 35 million users worldwide.",
                "Urban populations are expected to double by 2050.",
                "Air pollution from traffic causes 4.2 million deaths annually."
            ]
        }
    ]
};

/**
 * Get templates by category ID.
 * @param {string} categoryId - Category ID
 * @returns {Array} Templates for the category
 */
export function getTemplates(categoryId) {
    return SAMPLE_TOPIC_TEMPLATES[categoryId] || [];
}

/**
 * Get a specific template by ID.
 * @param {string} categoryId - Category ID
 * @param {string} templateId - Template ID
 * @returns {Object|null} Template or null if not found
 */
export function getTemplate(categoryId, templateId) {
    const templates = SAMPLE_TOPIC_TEMPLATES[categoryId];
    if (!templates) return null;
    return templates.find(t => t.id === templateId) || null;
}

/**
 * Search templates by name or hashtags.
 * @param {string} query - Search query
 * @returns {Array} Matching templates with category info
 */
export function searchTemplates(query) {
    const results = [];
    const lowerQuery = query.toLowerCase();

    for (const [categoryId, templates] of Object.entries(SAMPLE_TOPIC_TEMPLATES)) {
        const category = TOPIC_CATEGORIES.find(c => c.id === categoryId);

        for (const template of templates) {
            const nameMatch = template.name.toLowerCase().includes(lowerQuery);
            const hashtagMatch = template.hashtags.some(h => h.toLowerCase().includes(lowerQuery));

            if (nameMatch || hashtagMatch) {
                results.push({
                    ...template,
                    category: category
                });
            }
        }
    }

    return results;
}

export default {
    TOPIC_CATEGORIES,
    SAMPLE_TOPIC_TEMPLATES,
    getTemplates,
    getTemplate,
    searchTemplates
};
