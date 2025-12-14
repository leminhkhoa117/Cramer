/**
 * Text Diff Utility
 * Computes word-level differences between two texts for highlighting.
 * Uses a simple longest common subsequence (LCS) approach.
 */

/**
 * Tokenizes text into words and punctuation, preserving whitespace info
 * @param {string} text - Input text
 * @returns {Array<{token: string, isWord: boolean}>}
 */
export const tokenize = (text) => {
    if (!text) return [];
    // Match words (including contractions) or punctuation/whitespace
    const regex = /[\w']+|[^\w\s]+|\s+/g;
    const matches = text.match(regex) || [];
    return matches.map(token => ({
        token,
        isWord: /\w/.test(token),
    }));
};

/**
 * Computes Longest Common Subsequence between two arrays
 * @param {Array} a - First array
 * @param {Array} b - Second array
 * @returns {Array} - LCS array
 */
const computeLCS = (a, b) => {
    const m = a.length;
    const n = b.length;
    
    // Create DP table
    const dp = Array.from({ length: m + 1 }, () => Array(n + 1).fill(0));
    
    // Fill the table
    for (let i = 1; i <= m; i++) {
        for (let j = 1; j <= n; j++) {
            if (a[i - 1].toLowerCase() === b[j - 1].toLowerCase()) {
                dp[i][j] = dp[i - 1][j - 1] + 1;
            } else {
                dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
            }
        }
    }
    
    // Backtrack to find the LCS
    const lcs = [];
    let i = m, j = n;
    while (i > 0 && j > 0) {
        if (a[i - 1].toLowerCase() === b[j - 1].toLowerCase()) {
            lcs.unshift({ aIndex: i - 1, bIndex: j - 1, word: a[i - 1] });
            i--;
            j--;
        } else if (dp[i - 1][j] > dp[i][j - 1]) {
            i--;
        } else {
            j--;
        }
    }
    
    return lcs;
};

/**
 * Computes word-level diff between original and enhanced text
 * @param {string} original - Original text
 * @param {string} enhanced - Enhanced/improved text
 * @returns {Object} - { originalDiff, enhancedDiff } arrays of diff segments
 */
export const computeWordDiff = (original, enhanced) => {
    if (!original && !enhanced) {
        return { originalDiff: [], enhancedDiff: [] };
    }
    
    if (!original) {
        return {
            originalDiff: [],
            enhancedDiff: [{ type: 'add', text: enhanced }],
        };
    }
    
    if (!enhanced) {
        return {
            originalDiff: [{ type: 'remove', text: original }],
            enhancedDiff: [],
        };
    }
    
    // Tokenize both texts
    const origTokens = tokenize(original);
    const enhTokens = tokenize(enhanced);
    
    // Get only word tokens for LCS comparison
    const origWords = origTokens.filter(t => t.isWord).map(t => t.token);
    const enhWords = enhTokens.filter(t => t.isWord).map(t => t.token);
    
    // Compute LCS
    const lcs = computeLCS(origWords, enhWords);
    
    // Build sets of matched indices
    const origMatchedIndices = new Set(lcs.map(l => l.aIndex));
    const enhMatchedIndices = new Set(lcs.map(l => l.bIndex));
    
    // Build diff for original text
    let origWordIdx = 0;
    const originalDiff = [];
    let currentSegment = { type: 'same', text: '' };
    
    for (const token of origTokens) {
        if (token.isWord) {
            const isMatched = origMatchedIndices.has(origWordIdx);
            const newType = isMatched ? 'same' : 'remove';
            
            if (currentSegment.type !== newType && currentSegment.text) {
                originalDiff.push(currentSegment);
                currentSegment = { type: newType, text: '' };
            }
            currentSegment.type = newType;
            currentSegment.text += token.token;
            origWordIdx++;
        } else {
            // Preserve whitespace/punctuation with current segment type
            currentSegment.text += token.token;
        }
    }
    if (currentSegment.text) {
        originalDiff.push(currentSegment);
    }
    
    // Build diff for enhanced text
    let enhWordIdx = 0;
    const enhancedDiff = [];
    currentSegment = { type: 'same', text: '' };
    
    for (const token of enhTokens) {
        if (token.isWord) {
            const isMatched = enhMatchedIndices.has(enhWordIdx);
            const newType = isMatched ? 'same' : 'add';
            
            if (currentSegment.type !== newType && currentSegment.text) {
                enhancedDiff.push(currentSegment);
                currentSegment = { type: newType, text: '' };
            }
            currentSegment.type = newType;
            currentSegment.text += token.token;
            enhWordIdx++;
        } else {
            // Preserve whitespace/punctuation with current segment type
            currentSegment.text += token.token;
        }
    }
    if (currentSegment.text) {
        enhancedDiff.push(currentSegment);
    }
    
    return { originalDiff, enhancedDiff };
};

/**
 * Computes paragraph-by-paragraph diff
 * @param {string} originalEssay - Full original essay text
 * @param {Array} paragraphRewrites - Array of { original, improved } objects
 * @returns {Array} - Array of { originalParagraph, enhancedParagraph, hasDiff, originalDiff, enhancedDiff }
 */
export const computeParagraphDiffs = (originalEssay, paragraphRewrites) => {
    if (!originalEssay || !paragraphRewrites?.length) {
        return [];
    }
    
    // Split original essay into paragraphs
    const originalParagraphs = originalEssay
        .split('\n')
        .map(p => p.trim())
        .filter(p => p.length > 0);
    
    // Create a map of original paragraph text to its rewrite
    const rewriteMap = new Map();
    for (const rewrite of paragraphRewrites) {
        if (rewrite.original && rewrite.improved) {
            // Normalize for matching
            const normalizedOriginal = rewrite.original.trim().toLowerCase();
            rewriteMap.set(normalizedOriginal, rewrite.improved);
        }
    }
    
    // Process each paragraph
    return originalParagraphs.map((originalPara, idx) => {
        // Try to find matching rewrite
        let enhancedPara = null;
        
        // First try exact match
        const normalizedOrig = originalPara.trim().toLowerCase();
        if (rewriteMap.has(normalizedOrig)) {
            enhancedPara = rewriteMap.get(normalizedOrig);
        } else {
            // Try partial match (first 50 chars)
            const origStart = normalizedOrig.substring(0, 50);
            for (const [key, value] of rewriteMap) {
                if (key.startsWith(origStart) || origStart.startsWith(key.substring(0, 50))) {
                    enhancedPara = value;
                    break;
                }
            }
        }
        
        // If no rewrite found, enhanced is same as original
        if (!enhancedPara) {
            return {
                index: idx,
                originalParagraph: originalPara,
                enhancedParagraph: originalPara,
                hasDiff: false,
                originalDiff: [{ type: 'same', text: originalPara }],
                enhancedDiff: [{ type: 'same', text: originalPara }],
            };
        }
        
        // Compute diff
        const { originalDiff, enhancedDiff } = computeWordDiff(originalPara, enhancedPara);
        
        return {
            index: idx,
            originalParagraph: originalPara,
            enhancedParagraph: enhancedPara,
            hasDiff: true,
            originalDiff,
            enhancedDiff,
        };
    });
};

/**
 * Simple stats about differences
 * @param {Array} diffs - Array from computeParagraphDiffs
 * @returns {Object} - { totalChanges, additions, removals }
 */
export const getDiffStats = (diffs) => {
    let additions = 0;
    let removals = 0;
    
    for (const diff of diffs) {
        if (diff.hasDiff) {
            additions += diff.enhancedDiff.filter(d => d.type === 'add').length;
            removals += diff.originalDiff.filter(d => d.type === 'remove').length;
        }
    }
    
    return {
        totalChanges: additions + removals,
        additions,
        removals,
        paragraphsChanged: diffs.filter(d => d.hasDiff).length,
    };
};

export default {
    tokenize,
    computeWordDiff,
    computeParagraphDiffs,
    getDiffStats,
};
