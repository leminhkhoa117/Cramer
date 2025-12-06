import React, { useMemo } from 'react';
import '../../css/common/TabSwitcher.css';

/**
 * TabSwitcher - Reusable tab navigation component
 * @param {Object[]} tabs - Array of tab objects with id, label, and optional icon
 * @param {string} activeTab - Currently active tab id
 * @param {Function} onTabChange - Callback when tab is clicked
 * @param {string} className - Optional additional CSS class
 */
const TabSwitcher = ({ tabs, activeTab, onTabChange, className = '' }) => {
    const tabsWithDefaults = useMemo(() =>
        tabs.map(tab => ({
            ...tab,
            label: tab.label || tab.id,
        })),
        [tabs]
    );

    return (
        <div className={`tab-switcher ${className}`.trim()}>
            <nav className="tab-switcher__nav">
                {tabsWithDefaults.map(tab => {
                    const IconComponent = tab.icon;
                    return (
                        <button
                            key={tab.id}
                            type="button"
                            className={`tab-switcher__btn ${activeTab === tab.id ? 'active' : ''}`}
                            onClick={() => onTabChange(tab.id)}
                        >
                            {IconComponent && <IconComponent />}
                            {tab.label}
                        </button>
                    );
                })}
            </nav>
        </div>
    );
};

export default TabSwitcher;
