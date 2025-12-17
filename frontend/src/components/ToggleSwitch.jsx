import React from 'react';
import './../css/ToggleSwitch.css';

const ToggleSwitch = ({ id, checked, onChange, label }) => {
    return (
        <div className="toggle-switch-container">
            <label htmlFor={id} className="toggle-switch-label">{label}</label>
            <div className="toggle-switch">
                <input
                    type="checkbox"
                    id={id}
                    checked={checked}
                    onChange={(e) => onChange(e.target.checked)}
                    className="toggle-switch-checkbox"
                />
                <label htmlFor={id} className="toggle-switch-slider"></label>
            </div>
        </div>
    );
};

export default ToggleSwitch;
