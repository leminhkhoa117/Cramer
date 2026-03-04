/**
 * Skeleton - Loading placeholder components.
 * 
 * @since 2025-12-21 - Cat C Feature
 */

import React from 'react';
import './Skeleton.css';

/**
 * Basic skeleton line.
 */
export function SkeletonLine({
    width = '100%',
    height = 16,
    borderRadius = 4,
    className = ''
}) {
    return (
        <div
            className={`skeleton skeleton-line ${className}`}
            style={{ width, height, borderRadius }}
        />
    );
}

/**
 * Skeleton circle (for avatars).
 */
export function SkeletonCircle({
    size = 40,
    className = ''
}) {
    return (
        <div
            className={`skeleton skeleton-circle ${className}`}
            style={{ width: size, height: size }}
        />
    );
}

/**
 * Skeleton text block (multiple lines).
 */
export function SkeletonText({
    lines = 3,
    gap = 8,
    lastLineWidth = '60%',
    className = ''
}) {
    return (
        <div className={`skeleton-text ${className}`} style={{ gap }}>
            {Array.from({ length: lines }).map((_, i) => (
                <SkeletonLine
                    key={i}
                    width={i === lines - 1 ? lastLineWidth : '100%'}
                />
            ))}
        </div>
    );
}

/**
 * Skeleton card.
 */
export function SkeletonCard({
    className = '',
    hasImage = false,
    imageHeight = 120
}) {
    return (
        <div className={`skeleton-card ${className}`}>
            {hasImage && (
                <SkeletonLine height={imageHeight} borderRadius={8} />
            )}
            <div className="skeleton-card-content">
                <SkeletonLine width="70%" height={20} />
                <SkeletonText lines={2} />
            </div>
        </div>
    );
}

/**
 * Skeleton for form fields.
 */
export function SkeletonFormField({ className = '' }) {
    return (
        <div className={`skeleton-form-field ${className}`}>
            <SkeletonLine width={100} height={14} />
            <SkeletonLine height={44} borderRadius={10} />
        </div>
    );
}

/**
 * Skeleton for wizard step content.
 */
export function SkeletonWizardStep({ type = 'form', className = '' }) {
    if (type === 'cards') {
        return (
            <div className={`skeleton-wizard-step skeleton-cards-grid ${className}`}>
                {Array.from({ length: 4 }).map((_, i) => (
                    <div key={i} className="skeleton-skill-card">
                        <SkeletonCircle size={48} />
                        <SkeletonLine width="80%" height={16} />
                        <SkeletonLine width="60%" height={12} />
                    </div>
                ))}
            </div>
        );
    }

    return (
        <div className={`skeleton-wizard-step ${className}`}>
            <SkeletonLine width={200} height={24} />
            <div style={{ marginTop: 24 }}>
                <SkeletonFormField />
                <SkeletonFormField />
                <SkeletonFormField />
            </div>
        </div>
    );
}

/**
 * Skeleton for question preview.
 */
export function SkeletonQuestion({ className = '' }) {
    return (
        <div className={`skeleton-question ${className}`}>
            <div className="skeleton-question-header">
                <SkeletonLine width={40} height={24} borderRadius={6} />
                <SkeletonLine width={120} height={20} />
            </div>
            <SkeletonText lines={2} />
            <div className="skeleton-question-options">
                {Array.from({ length: 4 }).map((_, i) => (
                    <SkeletonLine key={i} width="90%" height={36} borderRadius={8} />
                ))}
            </div>
        </div>
    );
}

export default {
    Line: SkeletonLine,
    Circle: SkeletonCircle,
    Text: SkeletonText,
    Card: SkeletonCard,
    FormField: SkeletonFormField,
    WizardStep: SkeletonWizardStep,
    Question: SkeletonQuestion
};
