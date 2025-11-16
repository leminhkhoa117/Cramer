import React, { useRef, useEffect } from 'react';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';

const TestLayout = ({ showLeftPanel, leftPanelContent, children }) => {
    const leftPanelRef = useRef(null);

    // This effect is the source of truth for the panel's state.
    // It will run after the initial render and whenever showLeftPanel changes.
    useEffect(() => {
        const panel = leftPanelRef.current;
        if (panel) {
            if (showLeftPanel) {
                if (panel.isCollapsed()) {
                    panel.expand();
                }
            } else {
                if (!panel.isCollapsed()) {
                    panel.collapse();
                }
            }
        }
    }, [showLeftPanel]);

    return (
        <PanelGroup direction="horizontal" className="test-page-container">
            <Panel
                ref={leftPanelRef}
                collapsible={true}
                order={1}
                defaultSize={showLeftPanel ? 50 : 0}
                minSize={30}
            >
                <div className="passage-container">
                    {leftPanelContent}
                </div>
            </Panel>
            
            <PanelResizeHandle className={`resize-handle ${!showLeftPanel ? 'hidden' : ''}`}>
                <div className="resize-handle-icon-container">
                    <span className="resize-handle-icon">↔</span>
                </div>
            </PanelResizeHandle>

            <Panel minSize={30} order={2}>
                {children}
            </Panel>
        </PanelGroup>
    );
};

export default TestLayout;
