import React, { useRef, useEffect, useCallback } from 'react';
import { Group, Panel, Separator } from 'react-resizable-panels';

const MIN_LEFT_PANEL_SIZE = '30%';
const DEFAULT_LEFT_PANEL_SIZE = '50%';
const COLLAPSED_LEFT_PANEL_SIZE = '0%';

const TestLayout = ({ showLeftPanel, leftPanelContent, children, highlightContainerRef }) => { // Add highlightContainerRef
    const leftPanelRef = useRef(null);

    const handleLeftPanelCollapse = useCallback(() => {
        if (!showLeftPanel) return;
        const panel = leftPanelRef.current;
        if (panel) {
            setTimeout(() => panel.resize(MIN_LEFT_PANEL_SIZE), 0);
        }
    }, [showLeftPanel]);

    // This effect is the source of truth for the panel's state.
    // It will run after the initial render and whenever showLeftPanel changes.
    useEffect(() => {
        const panel = leftPanelRef.current;
        if (panel) {
            if (showLeftPanel) {
                if (panel.isCollapsed()) {
                    panel.expand();
                }
                panel.resize(MIN_LEFT_PANEL_SIZE);
            } else {
                if (!panel.isCollapsed()) {
                    panel.collapse();
                }
            }
        }
    }, [showLeftPanel]);

    return (
        <div className="test-page-container" ref={highlightContainerRef}> {/* Attach ref to a div */}
            <Group orientation="horizontal" className="panel-group-inner">
                <Panel
                    panelRef={leftPanelRef}
                    collapsible={true}
                    order={1}
                    collapsedSize={COLLAPSED_LEFT_PANEL_SIZE}
                    defaultSize={showLeftPanel ? DEFAULT_LEFT_PANEL_SIZE : COLLAPSED_LEFT_PANEL_SIZE}
                    minSize={MIN_LEFT_PANEL_SIZE}
                    onCollapse={handleLeftPanelCollapse}
                >
                    <div className="passage-container">
                        {leftPanelContent}
                    </div>
                </Panel>
                
                <Separator className={`resize-handle ${!showLeftPanel ? 'hidden' : ''}`}>
                    <div className="resize-handle-icon-container">
                        <span className="resize-handle-icon">↔</span>
                    </div>
                </Separator>

                <Panel minSize="30%" order={2}>
                    {children}
                </Panel>
            </Group>
        </div>
    );
};

export default TestLayout;
