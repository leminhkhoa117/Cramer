import React from 'react';
import { Group, Panel, Separator } from 'react-resizable-panels';
import ReviewColumn from './ReviewColumn';
import '../../css/common/review-layout-base.css';

const toPanelSize = (size, fallback) => {
    const value = size ?? fallback;
    return typeof value === 'number' ? `${value}%` : value;
};

/**
 * ReviewLayout - Unified resizable multi-panel layout for review pages
 * 
 * @param {Object} props
 * @param {Array} props.columns - Array of column configurations
 * @param {string} props.columns[].id - Unique column identifier
 * @param {Object} props.columns[].header - Column header config
 * @param {string} props.columns[].header.title - Header title
 * @param {ReactNode} props.columns[].header.icon - Optional icon component
 * @param {ReactNode} props.columns[].header.rightContent - Optional right-side content (e.g., word count)
 * @param {ReactNode} props.columns[].content - Column body content
 * @param {number} props.columns[].defaultSize - Default size percentage
 * @param {number} props.columns[].minSize - Minimum size percentage (default: 15)
 * @param {number} props.columns[].maxSize - Maximum size percentage (optional)
 * @param {string} props.className - Optional additional class name
 * @param {ReactNode} props.children - Optional children (rendered after panels)
 */
const ReviewLayout = ({ columns, className = '', children }) => {
    if (!columns || columns.length === 0) {
        return null;
    }

    return (
        <div className={`review-main-content ${className}`}>
            <Group orientation="horizontal" className="review-panel-group">
                {columns.map((column, index) => (
                    <React.Fragment key={column.id}>
                        <Panel
                            defaultSize={toPanelSize(column.defaultSize)}
                            minSize={toPanelSize(column.minSize, 15)}
                            maxSize={toPanelSize(column.maxSize)}
                        >
                            <ReviewColumn
                                id={column.id}
                                header={column.header}
                                className={column.className}
                            >
                                {column.content}
                            </ReviewColumn>
                        </Panel>

                        {/* Add resize handle between panels (not after last) */}
                        {index < columns.length - 1 && (
                            <Separator className="resize-handle">
                                <div className="resize-handle-icon-container">
                                    <span className="resize-handle-icon">↔</span>
                                </div>
                            </Separator>
                        )}
                    </React.Fragment>
                ))}
            </Group>
            {children}
        </div>
    );
};

export default ReviewLayout;
