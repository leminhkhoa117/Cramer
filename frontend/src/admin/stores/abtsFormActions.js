function getMaxPartsForSkill(skill) {
    if (skill === 'READING') return 3;
    if (skill === 'LISTENING') return 4;
    return 2;
}

function buildBalancedCounts(questionTypes, totalQuestions) {
    if (questionTypes.length === 0) return {};

    const counts = {};
    const baseCount = Math.floor(totalQuestions / questionTypes.length);
    let remainder = totalQuestions % questionTypes.length;

    questionTypes.forEach(type => {
        counts[type] = baseCount + (remainder > 0 ? 1 : 0);
        remainder--;
    });

    return counts;
}

export function createABTSFormActions(set, get, config) {
    const {
        initialFormState,
        READING_PART_TYPES,
        LISTENING_PART_TYPES,
        QUESTION_COUNTS
    } = config;

    const getTypePool = (skill, partNumber) => (
        skill === 'READING'
            ? READING_PART_TYPES[partNumber]
            : LISTENING_PART_TYPES[partNumber]
    );

    const buildRandomQuestionConfig = (skill, partNumber) => {
        const typePool = getTypePool(skill, partNumber);
        const totalQuestions = QUESTION_COUNTS[skill]?.[partNumber] || 10;
        const numTypes = Math.random() < 0.5 ? 2 : 3;
        const shuffled = [...typePool].sort(() => 0.5 - Math.random());
        const selectedTypes = shuffled.slice(0, numTypes);

        return {
            questionTypes: selectedTypes,
            questionTypeCounts: buildBalancedCounts(selectedTypes, totalQuestions)
        };
    };

    return {
        updateFormData: (updates) => {
            set(state => {
                const newFormData = { ...state.formData, ...updates };

                if (updates.skill && updates.skill !== state.formData.skill) {
                    const maxParts = getMaxPartsForSkill(updates.skill);
                    newFormData.selectedParts = (newFormData.selectedParts || [])
                        .filter(part => part >= 1 && part <= maxParts);

                    if (newFormData.partConfigs) {
                        const validConfigs = {};
                        Object.keys(newFormData.partConfigs).forEach(key => {
                            const partNumber = Number.parseInt(key, 10);
                            if (partNumber >= 1 && partNumber <= maxParts) {
                                validConfigs[key] = newFormData.partConfigs[key];
                            }
                        });
                        newFormData.partConfigs = validConfigs;
                    }
                }

                return { formData: newFormData };
            });
        },

        setFormField: (field, value) => {
            set(state => ({
                formData: { ...state.formData, [field]: value }
            }));
        },

        resetForm: () => {
            set({
                formData: { ...initialFormState },
                generationResult: null,
                generationError: null,
                currentStep: 1,
                audioUrls: {}
            });
        },

        setAudioUrl: (partNumber, url) => {
            set(state => ({
                audioUrls: { ...state.audioUrls, [partNumber]: url }
            }));
        },

        togglePartSelection: (partNumber) => {
            const { formData } = get();
            const currentParts = formData.selectedParts || [];
            const newParts = currentParts.includes(partNumber)
                ? currentParts.filter(part => part !== partNumber)
                : [...currentParts, partNumber].sort((leftPart, rightPart) => leftPart - rightPart);

            set({
                formData: {
                    ...formData,
                    selectedParts: newParts,
                    scope: 'MULTI_PART',
                    partNumber: newParts.length >= 1 ? newParts[0] : formData.partNumber
                }
            });
        },

        setPartConfig: (partNumber, config) => {
            const { formData } = get();
            const updatedConfigs = {
                ...formData.partConfigs,
                [partNumber]: {
                    ...(formData.partConfigs[partNumber] || {}),
                    ...config
                }
            };
            set({
                formData: { ...formData, partConfigs: updatedConfigs }
            });
        },

        applyGlobalConfigToAllParts: () => {
            const { formData } = get();
            const { selectedParts, topic, facts, questionTypes } = formData;
            const partConfigs = {};

            selectedParts.forEach(part => {
                partConfigs[part] = { topic, facts: [...facts], questionTypes: [...questionTypes] };
            });

            set({
                formData: { ...formData, partConfigs }
            });
        },

        clearPartSelections: () => {
            set(state => ({
                formData: {
                    ...state.formData,
                    selectedParts: [],
                    partConfigs: {},
                    scope: 'MULTI_PART'
                }
            }));
        },

        randomizePartConfig: (partNumber) => {
            const { formData } = get();
            const skill = formData.skill;

            if (!skill || skill === 'WRITING') return;

            const randomConfig = buildRandomQuestionConfig(skill, partNumber);
            const updatedConfigs = {
                ...formData.partConfigs,
                [partNumber]: {
                    ...(formData.partConfigs[partNumber] || {}),
                    ...randomConfig
                }
            };

            set({ formData: { ...formData, partConfigs: updatedConfigs } });
        },

        randomizeAllParts: () => {
            const { formData } = get();
            const skill = formData.skill;

            if (!skill || skill === 'WRITING') return;

            const updatedConfigs = { ...formData.partConfigs };

            formData.selectedParts.forEach(partNumber => {
                updatedConfigs[partNumber] = {
                    ...(formData.partConfigs[partNumber] || {}),
                    ...buildRandomQuestionConfig(skill, partNumber)
                };
            });

            set({ formData: { ...formData, partConfigs: updatedConfigs } });
        },

        togglePartQuestionType: (partNumber, typeId) => {
            const { formData } = get();
            const skill = formData.skill;
            const totalQuestions = QUESTION_COUNTS[skill]?.[partNumber] || 13;
            const partConfig = formData.partConfigs[partNumber] || { questionTypes: [], questionTypeCounts: {} };
            const currentTypes = partConfig.questionTypes || [];

            let newTypes;

            if (currentTypes.includes(typeId)) {
                newTypes = currentTypes.filter(type => type !== typeId);
            } else {
                if (currentTypes.length >= 3) return;
                newTypes = [...currentTypes, typeId];
            }

            const updatedConfigs = {
                ...formData.partConfigs,
                [partNumber]: {
                    ...partConfig,
                    questionTypes: newTypes,
                    questionTypeCounts: buildBalancedCounts(newTypes, totalQuestions)
                }
            };

            set({ formData: { ...formData, partConfigs: updatedConfigs } });
        },

        setPartTopic: (partNumber, topic) => {
            const { formData } = get();
            const updatedConfigs = {
                ...formData.partConfigs,
                [partNumber]: {
                    ...(formData.partConfigs[partNumber] || {}),
                    topic
                }
            };
            set({ formData: { ...formData, partConfigs: updatedConfigs } });
        },

        addPartFact: (partNumber, fact) => {
            const { formData } = get();
            const partConfig = formData.partConfigs[partNumber] || { facts: [] };
            const currentFacts = partConfig.facts || [];
            if (fact.trim() && currentFacts.length < 30) {
                const updatedConfigs = {
                    ...formData.partConfigs,
                    [partNumber]: {
                        ...partConfig,
                        facts: [...currentFacts, fact.trim()]
                    }
                };
                set({ formData: { ...formData, partConfigs: updatedConfigs } });
            }
        },

        removePartFact: (partNumber, index) => {
            const { formData } = get();
            const partConfig = formData.partConfigs[partNumber] || { facts: [] };
            const currentFacts = partConfig.facts || [];
            const updatedConfigs = {
                ...formData.partConfigs,
                [partNumber]: {
                    ...partConfig,
                    facts: currentFacts.filter((_, factIndex) => factIndex !== index)
                }
            };
            set({ formData: { ...formData, partConfigs: updatedConfigs } });
        },

        setPartPassageLength: (partNumber, length) => {
            const { formData } = get();
            const updatedConfigs = {
                ...formData.partConfigs,
                [partNumber]: {
                    ...(formData.partConfigs[partNumber] || {}),
                    passageLength: length
                }
            };
            set({ formData: { ...formData, partConfigs: updatedConfigs } });
        },

        updateGeneratedQuestion: (questionId, updates) => {
            const { generationResult } = get();
            if (!generationResult?.content?.questions) return;

            const updatedQuestions = generationResult.content.questions.map((question, index) => {
                const syntheticId = `abts-q-${index}`;
                if (questionId === question.id || questionId === syntheticId) {
                    return { ...question, ...updates };
                }
                return question;
            });

            set({
                generationResult: {
                    ...generationResult,
                    content: {
                        ...generationResult.content,
                        questions: updatedQuestions
                    }
                }
            });
        },

        addFact: (fact) => {
            const { formData } = get();
            if (fact.trim() && formData.facts.length < 30) {
                set({
                    formData: {
                        ...formData,
                        facts: [...formData.facts, fact.trim()]
                    }
                });
            }
        },

        removeFact: (index) => {
            const { formData } = get();
            set({
                formData: {
                    ...formData,
                    facts: formData.facts.filter((_, factIndex) => factIndex !== index)
                }
            });
        },

        toggleQuestionType: (typeId) => {
            const { formData } = get();
            const currentTypes = formData.questionTypes || [];
            const newTypes = currentTypes.includes(typeId)
                ? currentTypes.filter(type => type !== typeId)
                : [...currentTypes, typeId];

            const newCounts = { ...formData.questionTypeCounts };
            if (newTypes.includes(typeId) && !newCounts[typeId]) {
                newCounts[typeId] = 2;
            } else if (!newTypes.includes(typeId)) {
                delete newCounts[typeId];
            }

            set({
                formData: { ...formData, questionTypes: newTypes, questionTypeCounts: newCounts }
            });
        },

        setQuestionTypeCount: (typeId, count) => {
            const { formData } = get();
            const clampedCount = Math.max(1, Math.min(10, count));
            const currentTypes = formData.questionTypes || [];
            const newTypes = currentTypes.includes(typeId)
                ? currentTypes
                : [...currentTypes, typeId];

            const newCounts = {
                ...formData.questionTypeCounts,
                [typeId]: clampedCount
            };

            set({
                formData: {
                    ...formData,
                    questionTypes: newTypes,
                    questionTypeCounts: newCounts
                }
            });
        },

        loadTemplate: (template) => {
            set(state => ({
                formData: {
                    ...state.formData,
                    topic: template.name,
                    hashtags: template.hashtags || [],
                    facts: template.facts || []
                }
            }));
        }
    };
}