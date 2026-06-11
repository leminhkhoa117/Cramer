package com.cramer.abts.generation;

import com.cramer.abts.web.dto.PartConfig;
import com.cramer.platform.common.ielts.Skill;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Generates one part/task through its phase pipeline (SPEC-21 §3) and returns the merged section
 * content (passage/transcript + questions/answers, or writing task + sample + band). Implementations
 * are stateless Spring beans; per-request state lives in {@link GenerationContext}.
 */
public interface PartGenerator {

    Skill skill();

    /** Run the phase pipeline for one part and return the merged section JSON. */
    ObjectNode generatePart(int part, PartConfig config, GenerationContext ctx);
}
