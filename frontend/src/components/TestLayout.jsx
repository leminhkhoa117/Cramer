import React, { useRef, useEffect, useCallback } from 'react';
import { Panel, PanelGroup, PanelResizeHandle } from 'react-resizable-panels';

const MIN_LEFT_PANEL_SIZE = 30;

const TestLayout = ({ showLeftPanel, leftPanelContent, children, highlightContainerRef }) => { // Add highlightContainerRef
    const leftPanelRef = useRef(null);

    const handleLeftPanelCollapse = useCallback(() => {
        if (!showLeftPanel) return;
        const panel = leftPanelRef.current;
        if (panel) {
            setTimeout(() => panel.expand(MIN_LEFT_PANEL_SIZE), 0);
        }
    }, [showLeftPanel]);

    // This effect is the source of truth for the panel's state.
    // It will run after the initial render and whenever showLeftPanel changes.
    useEffect(() => {
        const panel = leftPanelRef.current;
        if (panel) {
            if (showLeftPanel) {
                if (panel.isCollapsed()) {
                    panel.expand(MIN_LEFT_PANEL_SIZE);
                }
            } else {
                if (!panel.isCollapsed()) {
                    panel.collapse();
                }
            }
        }
    }, [showLeftPanel]);

    return (
        <div className="test-page-container" ref={highlightContainerRef}> {/* Attach ref to a div */}
            <PanelGroup direction="horizontal" className="panel-group-inner"> {/* PanelGroup can be inside */}
                <Panel
                    ref={leftPanelRef}
                    collapsible={true}
                    order={1}
                    defaultSize={showLeftPanel ? 50 : 0}
                    minSize={MIN_LEFT_PANEL_SIZE}
                    onCollapse={handleLeftPanelCollapse}
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
        </div>
    );
};

export default TestLayout;
