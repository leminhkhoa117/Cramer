package com.cramer.service.abts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

final class AbtsTemplateCatalogService {

    private static final Logger logger = LoggerFactory.getLogger(AbtsTemplateCatalogService.class);

    private final JdbcTemplate jdbcTemplate;

    AbtsTemplateCatalogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<Map<String, Object>> getTemplateCategories() {
        try {
            String sql = """
                    SELECT DISTINCT
                        category as id,
                        category_label as name,
                        category_label as name_vi,
                        category_icon as emoji,
                        COUNT(*) as template_count
                    FROM public.abts_templates
                    WHERE is_active = true
                    GROUP BY category, category_label, category_icon
                    ORDER BY category
                    """;

            List<Map<String, Object>> categories = jdbcTemplate.queryForList(sql);

            if (categories != null && !categories.isEmpty()) {
                logger.info("Fetched {} template categories from database", categories.size());
                return categories;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch template categories from database, using fallback: {}", e.getMessage());
        }

        return List.of(
                Map.of("id", "environment", "emoji", "🌍", "name", "Environment", "name_vi", "Môi trường"),
                Map.of("id", "technology", "emoji", "💻", "name", "Technology", "name_vi", "Công nghệ"),
                Map.of("id", "education", "emoji", "📚", "name", "Education", "name_vi", "Giáo dục"),
                Map.of("id", "health", "emoji", "🏥", "name", "Health & Medicine", "name_vi", "Y tế"),
                Map.of("id", "society", "emoji", "👥", "name", "Society", "name_vi", "Xã hội"),
                Map.of("id", "business", "emoji", "💼", "name", "Business & Economy", "name_vi", "Kinh doanh"),
                Map.of("id", "science", "emoji", "🔬", "name", "Science", "name_vi", "Khoa học"),
                Map.of("id", "history", "emoji", "🏛️", "name", "History", "name_vi", "Lịch sử"),
                Map.of("id", "arts", "emoji", "🎨", "name", "Arts & Culture", "name_vi", "Nghệ thuật"),
                Map.of("id", "travel", "emoji", "✈️", "name", "Travel & Tourism", "name_vi", "Du lịch"));
    }

    List<Map<String, Object>> getTemplatesByCategory(String categoryId) {
        try {
            String sql = """
                    SELECT
                        id::text as id,
                        topic as name,
                        description,
                        hashtags,
                        facts,
                        skill,
                        difficulty,
                        test_type as "testType",
                        suggested_question_types as "suggestedQuestionTypes",
                        is_featured as "isFeatured",
                        use_count as "useCount"
                    FROM public.abts_templates
                    WHERE category = ? AND is_active = true
                    ORDER BY is_featured DESC, use_count DESC, topic
                    """;

            List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql, categoryId);

            if (templates != null && !templates.isEmpty()) {
                logger.info("Fetched {} templates for category '{}' from database", templates.size(), categoryId);
                return templates;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch templates for category '{}' from database: {}", categoryId, e.getMessage());
        }

        return List.of(
                Map.of(
                        "id", categoryId + "_template_1",
                        "name", "Sample Template 1",
                        "hashtags", List.of("sample", categoryId),
                        "facts", List.of(
                                "This is a sample fact for the template.",
                                "Templates will be populated with more content.")));
    }

    void incrementTemplateUseCount(String templateId) {
        try {
            String sql = """
                    UPDATE public.abts_templates
                    SET use_count = use_count + 1, last_used_at = NOW()
                    WHERE id = ?::uuid
                    """;
            jdbcTemplate.update(sql, templateId);
            logger.debug("Incremented use count for template: {}", templateId);
        } catch (Exception e) {
            logger.warn("Failed to increment template use count: {}", e.getMessage());
        }
    }
}