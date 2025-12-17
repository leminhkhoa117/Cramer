import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FaHighlighter, FaStrikethrough, FaUnderline, FaTrashAlt } from 'react-icons/fa';
import '../css/HighlightPopup.css'; // Local styles for popup controls

const HighlightPopup = ({ x, y, visible, applyHighlight, clearHighlight }) => {
    const colors = ['#FFFF00', '#ADD8E6', '#90EE90', '#FFB6C1', '#D3D3D3']; // Yellow, LightBlue, LightGreen, LightPink, LightGray

    if (!visible) return null;

    return (
        <AnimatePresence>
            {visible && (
                <motion.div
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.8 }}
                    transition={{ duration: 0.1 }}
                    className="highlight-popup"
                    style={{ left: x, top: y }}
                >
                    {colors.map(color => (
                        <button
                            key={color}
                            className="highlight-button color-button"
                            style={{ backgroundColor: color }}
                            onClick={() => applyHighlight({ backgroundColor: color })}
                            title={`Highlight with ${color}`}
                        />
                    ))}
                    <button
                        className="highlight-button"
                        onClick={() => applyHighlight({ textDecoration: 'line-through' })}
                        title="Strikethrough"
                    >
                        <FaStrikethrough />
                    </button>
                    <button
                        className="highlight-button"
                        onClick={() => applyHighlight({ textDecoration: 'underline' })}
                        title="Underline"
                    >
                        <FaUnderline />
                    </button>
                    <button
                        className="highlight-button clear-button"
                        onClick={clearHighlight}
                        title="Clear Highlight"
                    >
                        <FaTrashAlt />
                    </button>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

export default HighlightPopup;
