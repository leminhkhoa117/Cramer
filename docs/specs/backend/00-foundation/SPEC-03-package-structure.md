# SPEC-03 — Package Structure

> Status: **Authoritative** · Depends on: SPEC-01, SPEC-02
> The complete target tree. Scaffolding (todo #5) creates this skeleton. Files listed are
> representative, not exhaustive; module specs (`10-modules/`) own the full contents.

---

## 1. Root

```
com.cramer
├── CramerApplication.java          # @SpringBootApplication, @EnableAsync, @EnableScheduling
├── platform/                       # shared kernel (§2)
├── identity/                       # §3
├── catalog/                        # §4
├── assessment/                     # §5
├── writing/                        # §6
├── speaking/                       # §7
├── billing/                        # §8
├── engagement/                     # §9
├── admin/                          # §10
└── abts/                           # §11
```

## 2. `platform` — shared kernel

```
platform
├── security
│   ├── SupabaseJwtConfig.java        # NimbusJwtDecoder from Supabase HS256 secret (OAuth2 resource server)
│   ├── SecurityConfig.java          # filter chain, route authz, admin gate
│   ├── AdminAuthorizationService.java  # is profiles.is_admin for /api/admin/**
│   └── CurrentUser.java             # resolves authenticated UUID (replaces BaseController)
├── web
│   ├── GlobalExceptionHandler.java  # exception → ApiError (SPEC-04 §2)
│   ├── ApiError.java                # standard error body (record)
│   ├── PageResponse.java            # paging wrapper (record)
│   ├── WebConfig.java               # CORS + Jackson ObjectMapper
│   └── HealthController.java        # /api/health readiness/liveness (replaces debug ctrls)
├── error
│   ├── ResourceNotFoundException.java
│   ├── ResourceAlreadyExistsException.java
│   ├── OperationNotAllowedException.java
│   ├── QuotaExceededException.java
│   ├── RateLimitExceededException.java
│   └── PayloadTooLargeException.java   # now mapped → 413 (SPEC-04)
├── integration
│   ├── openrouter
│   │   ├── OpenRouterClient.java       # chat, json-schema, SSE stream, models
│   │   ├── OpenRouterProperties.java
│   │   └── OpenRouterError.java        # AUTH_FAILED, INSUFFICIENT_CREDITS, ...
│   ├── llm
│   │   ├── DeepSeekClient.java         # OpenAI-compatible /chat/completions
│   │   └── LlmProperties.java
│   └── supabase
│       ├── SupabaseStorageClient.java  # download/upload/exists/publicUrl
│       ├── SupabaseAdminClient.java    # auth admin (email existence)
│       └── SupabaseProperties.java
├── ratelimit
│   ├── RateLimiter.java               # Bucket4j by userId:endpointType
│   └── RateLimitProperties.java
├── config
│   ├── DataSourceConfig.java          # conditional Hikari
│   ├── OpenApiConfig.java
│   └── StartupValidator.java          # fail-fast on missing secrets
└── common
    ├── json
    │   ├── Json.java                  # static serialize/deserialize helper
    │   └── JsonbConverters.java
    ├── ielts
    │   ├── BandScale.java             # raw-correct → IELTS band
    │   ├── Skill.java                 # shared IELTS skill vocabulary (READING/LISTENING/WRITING/SPEAKING)
    │   └── QuestionType.java          # shared IELTS question-type vocabulary
    └── ids
        └── Ids.java                   # UUID parsing/validation
```

## 3. `identity`

```
identity
├── web
│   ├── AuthController.java            # POST /api/auth/check-email
│   ├── ProfileController.java         # GET/PUT /api/profiles/{id} (self only)
│   └── dto
│       ├── CheckEmailRequest.java
│       ├── ProfileResponse.java
│       └── UpdateProfileRequest.java
├── service
│   ├── ProfileService.java
│   └── EmailLookupService.java        # wraps SupabaseAdminClient
├── domain
│   └── Profile.java                   # profiles table
└── repository
    └── ProfileRepository.java
```
> `target` lives in `engagement` (dashboard owns goals) — see §9.

## 4. `catalog`

```
catalog
├── web
│   ├── TestDeliveryController.java    # GET /api/tests/data (safe, no answers)
│   ├── CourseController.java          # GET /api/courses... (published browse)
│   ├── admin
│   │   ├── AdminTestSetController.java
│   │   ├── AdminTestController.java
│   │   ├── AdminSectionController.java
│   │   ├── AdminQuestionController.java
│   │   └── AdminHashtagController.java
│   └── dto
│       ├── TestSectionView.java        # answer-free
│       ├── CreateTestSetRequest.java
│       ├── CreateTestRequest.java
│       ├── SectionRequest.java / QuestionRequest.java
│       └── ...
├── service
│   ├── TestDeliveryService.java        # safe test payloads
│   ├── CourseQueryService.java         # published sets/tests browse
│   ├── TestSetService.java             # admin CRUD + publish + reorder
│   ├── TestAdminService.java           # test CRUD, publish, duplicate, hashtags
│   ├── SectionService.java
│   ├── QuestionService.java
│   ├── HashtagService.java
│   └── ContentLookupPort.java          # published: resolve sections/questions for a test+skill
├── domain
│   ├── TestSet.java  Test.java  Section.java  Question.java  Hashtag.java
│   ├── SectionStatus.java  Difficulty.java  SkillConverter.java
│   │   # Skill + QuestionType are shared-kernel (platform.common.ielts), not catalog-owned
├── repository
│   ├── TestSetRepository.java  TestRepository.java
│   ├── SectionRepository.java  QuestionRepository.java  HashtagRepository.java
└── config
```

## 5. `assessment`

```
assessment
├── web
│   ├── AttemptController.java         # start/progress/submit/cancel/resume/review/regrade
│   └── dto
│       ├── StartAttemptRequest.java  SaveProgressRequest.java  SubmitAnswersRequest.java
│       ├── AttemptResultResponse.java  AttemptReviewView.java  AnswerView.java
├── service
│   ├── AttemptService.java            # lifecycle + locking
│   ├── ScoringService.java            # answer normalization + set comparison (SPEC-12)
│   └── AttemptReviewService.java
├── domain
│   ├── Attempt.java  UserAnswer.java  AttemptStatus.java
├── repository
│   ├── AttemptRepository.java  UserAnswerRepository.java
```

## 6. `writing`

```
writing
├── web
│   ├── WritingController.java         # draft/submit/status/review/regrade/validate-key
│   └── dto
│       ├── SubmitEssayRequest.java  WritingStatusResponse.java  WritingReviewView.java
├── service
│   ├── WritingSubmissionService.java  # lifecycle, quota gate, weighted band
│   ├── WritingGradingService.java     # DeepSeek prompt/call/parse (was LLMGradingService)
│   ├── WritingGradingDispatcher.java  # @Async, post-commit, parallel tasks
│   └── WritingPromptBuilder.java
├── domain
│   ├── WritingSubmission.java  WritingStatus.java
├── repository
│   └── WritingSubmissionRepository.java
└── config
    └── WritingAsyncConfig.java
```

## 7. `speaking`

```
speaking
├── web
│   ├── SpeakingController.java        # sessions/transcripts/complete/abandon/status/results/history
│   ├── ws
│   │   ├── SpeakingWebSocketConfig.java
│   │   ├── SpeakingWebSocketHandler.java
│   │   └── SpeakingHandshakeInterceptor.java   # JWT at handshake
│   └── dto
│       ├── CreateSessionRequest.java  SaveTranscriptRequest.java
│       ├── SpeakingSessionView.java  SpeakingResultView.java  SpeakingHistoryItemView.java
├── service
│   ├── SpeakingSessionService.java    # lifecycle, transcripts, complete, results, history
│   ├── SpeakingSessionStateMachine.java  # allowed transitions (regrade fix, SPEC-14)
│   ├── SpeakingBlueprintService.java  # build frozen blueprint from catalog content
│   ├── selection
│   │   ├── SelectionPlanner.java      # interface (2 impls)
│   │   ├── HeuristicSelectionPlanner.java
│   │   ├── LlmSelectionPlanner.java
│   │   └── SelectionPromptBuilder.java
│   ├── grading
│   │   ├── SpeakingGradingDispatcher.java  # @Async dispatch
│   │   ├── SpeakingGradingWorker.java      # OpenRouter call, retry, persist, refund
│   │   ├── SpeakingAudioPreparer.java      # Supabase download + ffmpeg transcode
│   │   ├── SpeakingGradingPromptBuilder.java
│   │   └── SpeakingGradingWatchdog.java    # @Scheduled stuck-session recovery
│   ├── realtime
│   │   ├── GeminiLiveClient.java
│   │   └── GeminiLiveConnection.java
│   └── AdminSpeakingService.java      # regrade + audit (used by admin module port)
├── domain
│   ├── SpeakingSession.java  SpeakingTranscript.java  SpeakingSessionStatus.java
│   └── grading
│       └── SpeakingGradingResult.java # schema-driven typed result (SPEC-14)
├── repository
│   ├── SpeakingSessionRepository.java  SpeakingTranscriptRepository.java
└── config
    ├── SpeakingSessionProperties.java  SpeakingSelectionProperties.java
    ├── SpeakingGeminiProperties.java   SpeakingGradingProperties.java
    └── SpeakingAsyncConfig.java
```

## 8. `billing`

```
billing
├── web
│   ├── SubscriptionController.java  CreditController.java  QuotaController.java
│   ├── PaymentController.java        # incl. public webhook
│   └── dto (…Request/…Response/…View)
├── service
│   ├── SubscriptionService.java
│   ├── CreditService.java            # idempotent earn/spend/refund (SPEC-15)
│   ├── LuaPackService.java           # DB-driven packs only
│   ├── QuotaService.java             # monthly quota rows, tier-aware status
│   ├── AttemptBillingService.java    # attempt overage (was QuotaBilling)
│   ├── ChatBillingService.java       ├── TranslationBillingService.java
│   ├── FeatureAccessService.java     # wired into request paths (SPEC-15)
│   ├── PaymentService.java           # PayOS create + idempotent webhook grant
│   ├── SubscriptionExpiryScheduler.java
│   └── ports
│       ├── UsageBillingPort.java      # chargeAiGrading/refund (writing/speaking)
│       ├── AttemptBillingPort.java    # preCheck/charge attempt (assessment)
│       └── SpeakingBillingPort.java   # check/deduct/refund Lúa (speaking)
├── domain
│   ├── Subscription.java  SubscriptionTier.java  UserCredit.java  CreditTransaction.java
│   ├── LuaPack.java  UserQuota.java  SkillQuota.java  PaymentOrder.java  TranslationUsage.java
│   ├── CreditCategory.java (incl. ADMIN_ADJUSTMENT)  TransactionType.java  PaymentStatus.java
├── repository (one per aggregate)
└── config
    └── PayOsProperties.java
```

## 9. `engagement`

```
engagement
├── web
│   ├── ChatController.java  VocabularyController.java
│   ├── DashboardController.java       # summary, course-history, target
│   └── dto (…)
├── service
│   ├── ChatService.java               # DeepSeek chat (charge-after-success)
│   ├── VocabularyService.java         # CRUD + translate
│   ├── DashboardService.java          # aggregation/read-model
│   ├── TargetService.java             # IELTS goals
│   └── ActivityService.java           # user_activities logging (port for other modules)
├── domain
│   ├── ChatMessage.java  ChatbotUsage.java  Vocabulary.java  Target.java  UserActivity.java
├── repository (…)
```

## 10. `admin`

```
admin
├── web
│   ├── AdminUserController.java  AdminDashboardController.java
│   ├── AdminFinanceController.java  AdminActivityController.java
│   └── dto (…)
├── service
│   ├── AdminUserService.java          # list/detail/status/credit/subscription (via billing ports)
│   ├── AdminDashboardService.java     # cross-domain counts (read-only projections)
│   ├── AdminFinanceService.java       # reports/exports (read-only)
│   └── AuditService.java              # admin_audit_log writer (port for other modules)
├── domain
│   └── AdminAuditLog.java
└── repository
    └── AdminAuditLogRepository.java
```

## 11. `abts`

```
abts
├── web
│   ├── AbtsGenerationController.java   # /api/admin/abts/generate(/stream)
│   ├── AbtsRefinementController.java   # /refine/stream, /refine/apply
│   ├── AbtsCatalogController.java      # /models, /templates, /status, /validate, /save
│   └── dto (GenerationRequest, StreamEvent, RefinementRequest, SaveContentRequest, ...)
├── service
│   ├── GenerationService.java          # orchestration facade
│   ├── generation
│   │   ├── ReadingGenerator.java  ListeningGenerator.java  WritingGenerator.java
│   │   ├── MultiPartGenerator.java     # phases, merge, renumber, partial success
│   │   └── StreamingSession.java       # SSE lifecycle, cancellation
│   ├── prompt
│   │   ├── ReadingPromptBuilder.java  ListeningPromptBuilder.java  WritingPromptBuilder.java
│   │   ├── PromptSchemaBuilder.java  PromptFragments.java
│   ├── validation
│   │   ├── ContentValidator.java  ReadingValidator.java  ListeningValidator.java  WritingValidator.java
│   │   └── ValidationResult.java  ValidationIssue.java
│   ├── refinement
│   │   ├── RefinementService.java  RefinementPromptBuilder.java
│   │   ├── JsonPatcher.java  HunkBuilder.java  HunkApplier.java
│   ├── model
│   │   ├── ModelCatalogService.java  ModelCapabilityRegistry.java
│   └── persistence
│       └── GeneratedContentSaver.java  # writes into catalog tables (draft)
├── domain                              # value types only; reuses catalog tables for save
│   ├── QuestionRange.java  GenerationStatus.java  Phase.java
├── config
│   └── AbtsStreamingProperties.java  AbtsStreamingAsyncConfig.java
└── resources (src/main/resources/abts/...)  # prompt + schema assets
```

## 12. Test tree

`src/test/java/com/cramer/<module>/…` mirrors the above (SPEC-02 §6).

## 13. Change log

| Date | Change |
|------|--------|
| 10/06/2026 | Initial authoring. |
| 10/06/2026 | Reconciled `platform.security` tree with SPEC-04 §1.2 / SPEC-18 §1: replaced `JwtAuthFilter` + `SupabaseJwtService` with `SupabaseJwtConfig` (OAuth2 resource server). |
| 11/06/2026 | Moved `Skill` + `QuestionType` from `catalog.domain` to `platform.common.ielts` (shared kernel) so assessment/speaking/abts depend on platform, not catalog's domain (SPEC-01 §3). |
