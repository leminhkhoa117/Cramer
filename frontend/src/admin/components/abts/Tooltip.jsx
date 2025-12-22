/**
 * Tooltip - Simple tooltip component for help text.
 * 
 * @since 2025-12-21 - Cat C Feature
 */

import React, { useState, useRef, useEffect } from 'react';
import { FiHelpCircle, FiInfo } from 'react-icons/fi';
import './Tooltip.css';

export default function Tooltip({
    content,
    children,
    position = 'top', // 'top' | 'bottom' | 'left' | 'right'
    icon = 'help', // 'help' | 'info' | 'none'
    trigger = 'hover', // 'hover' | 'click'
    maxWidth = 250,
    delay = 200
}) {
    const [isVisible, setIsVisible] = useState(false);
    const [coords, setCoords] = useState({ x: 0, y: 0 });
    const triggerRef = useRef(null);
    const tooltipRef = useRef(null);
    const timeoutRef = useRef(null);

    // Calculate position
    useEffect(() => {
        if (isVisible && triggerRef.current && tooltipRef.current) {
            const trigger = triggerRef.current.getBoundingClientRect();
            const tooltip = tooltipRef.current.getBoundingClientRect();

            let x = 0, y = 0;

            switch (position) {
                case 'top':
                    x = trigger.left + (trigger.width - tooltip.width) / 2;
                    y = trigger.top - tooltip.height - 8;
                    break;
                case 'bottom':
                    x = trigger.left + (trigger.width - tooltip.width) / 2;
                    y = trigger.bottom + 8;
                    break;
                case 'left':
                    x = trigger.left - tooltip.width - 8;
                    y = trigger.top + (trigger.height - tooltip.height) / 2;
                    break;
                case 'right':
                    x = trigger.right + 8;
                    y = trigger.top + (trigger.height - tooltip.height) / 2;
                    break;
            }

            // Keep within viewport
            x = Math.max(8, Math.min(x, window.innerWidth - tooltip.width - 8));
            y = Math.max(8, Math.min(y, window.innerHeight - tooltip.height - 8));

            setCoords({ x, y });
        }
    }, [isVisible, position]);

    const showTooltip = () => {
        if (trigger === 'hover') {
            timeoutRef.current = setTimeout(() => setIsVisible(true), delay);
        } else {
            setIsVisible(true);
        }
    };

    const hideTooltip = () => {
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
        }
        setIsVisible(false);
    };

    const toggleTooltip = () => {
        setIsVisible(!isVisible);
    };

    // Close on click outside for click trigger
    useEffect(() => {
        if (trigger === 'click' && isVisible) {
            const handleClickOutside = (e) => {
                if (triggerRef.current && !triggerRef.current.contains(e.target) &&
                    tooltipRef.current && !tooltipRef.current.contains(e.target)) {
                    setIsVisible(false);
                }
            };
            document.addEventListener('mousedown', handleClickOutside);
            return () => document.removeEventListener('mousedown', handleClickOutside);
        }
    }, [trigger, isVisible]);

    const handlers = trigger === 'hover'
        ? { onMouseEnter: showTooltip, onMouseLeave: hideTooltip }
        : { onClick: toggleTooltip };

    const IconComponent = icon === 'help' ? FiHelpCircle : icon === 'info' ? FiInfo : null;

    return (
        <span className="tooltip-wrapper" ref={triggerRef} {...handlers}>
            {children || (
                IconComponent && <IconComponent className="tooltip-icon" size={14} />
            )}

            {isVisible && content && (
                <div
                    ref={tooltipRef}
                    className={`tooltip tooltip-${position}`}
                    style={{
                        left: coords.x,
                        top: coords.y,
                        maxWidth
                    }}
                >
                    <div className="tooltip-content">{content}</div>
                    <div className={`tooltip-arrow tooltip-arrow-${position}`} />
                </div>
            )}
        </span>
    );
}

/**
 * FormFieldWithTooltip - Form field wrapper with integrated tooltip.
 */
export function FormFieldWithTooltip({
    label,
    tooltip,
    required,
    children,
    className = ''
}) {
    return (
        <div className={`form-field-with-tooltip ${className}`}>
            <div className="field-label-row">
                <label className="field-label">
                    {label}
                    {required && <span className="required-star">*</span>}
                </label>
                {tooltip && (
                    <Tooltip content={tooltip} position="top" icon="help">
                        <FiHelpCircle className="field-help-icon" size={14} />
                    </Tooltip>
                )}
            </div>
            {children}
        </div>
    );
}
