import React from 'react';

// A simple layout for the test page to provide a distraction-free environment
export default function TestLayout({ children }) {
    return (
        <div className="test-layout">
            {children}
        </div>
    );
}
