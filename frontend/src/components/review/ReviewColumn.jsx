import React from 'react';
import '../../css/ReviewColumn.css';

/**
 * ReviewColumn - Single column component for review layout
 * 
 * @param {Object} props
 * @param {string} props.id - Column identifier
 * @param {Object} props.header - Header configuration
 * @param {string} props.header.title - Header title text
 * @param {ReactNode} props.header.icon - Optional icon
 * @param {ReactNode} props.header.rightContent - Optional content on right side (e.g., word count)
 * @param {ReactNode} props.children - Column body content
 * @param {string} props.className - Optional additional class name
 */
const ReviewColumn = ({ id, header, children, className = '' }) => {
    // Determine border radius class based on column position
    const getColumnClass = () => {
        const baseClass = 'review-column';
        const classes = [baseClass];

        if (className) {
            classes.push(className);
        }

        // Add id-based class for specific styling
        if (id) {
            classes.push(`${id}-column`);
        }

        return classes.join(' ');
    };

    return (
        <div className={getColumnClass()}>
            {header && (
                <div className="column-header">
                    <h3>
                        {header.icon && <span className="header-icon">{header.icon}</span>}
                        {header.title}
                    </h3>
                    {header.rightContent && (
                        <div className="header-right-content">
                            {header.rightContent}
                        </div>
                    )}
                </div>
            )}
            <div className="column-content">
                {children}
            </div>
        </div>
    );
};

export default ReviewColumn;
