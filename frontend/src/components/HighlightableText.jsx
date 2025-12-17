import React from 'react';
import HighlightableHtmlContent from './HighlightableHtmlContent'; // Import the new component

const HighlightableText = ({ text, contentId }) => { // Add contentId prop
    return (
        <HighlightableHtmlContent
            htmlString={text}
            contentId={contentId} // Pass the contentId
            className="passage-content"
        />
    );
};

export default HighlightableText;