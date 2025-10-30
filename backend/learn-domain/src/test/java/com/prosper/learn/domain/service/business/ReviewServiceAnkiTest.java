package com.prosper.learn.domain.service.business;

import com.prosper.learn.common.config.SystemProperties;
import com.prosper.learn.domain.service.data.*;
import com.prosper.learn.dto.request.ReviewCardRequest;
import com.prosper.learn.persistence.dataobject.UserCardSrsDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.prosper.learn.persistence.dataobject.UserCardSrsDO.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ReviewService Anki 算法单元测试
 * 测试覆盖所有状态和评级的组合
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService - Anki 算法测试")
class ReviewServiceAnkiTest {

    @Mock
    private UserCardSrsDataService srsStateDataService;

    @Mock
    private MemoryCardDataService cardDataService;

    @Mock
    private MemoryCardService memoryCardService;

    @Mock
    private UserDataService userDataService;

    @Mock
    private SystemProperties systemProperties;

    @InjectMocks
    private ReviewService reviewService;

    private SystemProperties.Srs.Algorithm algorithmConfig;

    @BeforeEach
    void setUp() {
        // 初始化默认配置
        algorithmConfig = new SystemProperties.Srs.Algorithm();
        algorithmConfig.setLearningSteps(new int[]{10, 60});
        algorithmConfig.setRelearningSteps(new int[]{20});
        algorithmConfig.setGraduatingInterval(1);
        algorithmConfig.setEasyInterval(4);
        algorithmConfig.setEasyBonus(1.3);
        algorithmConfig.setNewIntervalMultiplier(0.5);
        algorithmConfig.setMinEaseFactor(1.3);

        SystemProperties.Srs srs = new SystemProperties.Srs();
        srs.setAlgorithm(algorithmConfig);

        when(systemProperties.getSrs()).thenReturn(srs);
    }

    // ========== handleNewCard() 测试 ==========

    @Test
    @DisplayName("NEW卡片 - 评级1(重来) - 进入LEARNING step=0")
    void testNewCard_Rating1_EntersLearning() {
        // Given
        UserCardSrsDO card = createNewCard();
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(1);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_LEARNING, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(10, updated.getInterval());  // learningSteps[0]
        assertNotNull(updated.getReviewDueAt());
    }

    @Test
    @DisplayName("NEW卡片 - 评级2(困难) - 进入LEARNING step=0")
    void testNewCard_Rating2_EntersLearning() {
        // Given
        UserCardSrsDO card = createNewCard();
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(2);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_LEARNING, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(10, updated.getInterval());
    }

    @Test
    @DisplayName("NEW卡片 - 评级3(良好) - 跳到LEARNING step=1")
    void testNewCard_Rating3_SkipsToStep1() {
        // Given
        UserCardSrsDO card = createNewCard();
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(3);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_LEARNING, updated.getType());
        assertEquals((byte) 1, updated.getCurrentStep());
        assertEquals(60, updated.getInterval());  // learningSteps[1]
    }

    @Test
    @DisplayName("NEW卡片 - 评级3(良好) 单步骤配置 - 直接毕业")
    void testNewCard_Rating3_SingleStep_Graduates() {
        // Given
        algorithmConfig.setLearningSteps(new int[]{10});  // 只有一步
        UserCardSrsDO card = createNewCard();
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(3);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(1, updated.getInterval());  // graduatingInterval
    }

    @Test
    @DisplayName("NEW卡片 - 评级4(简单) - 立即毕业到REVIEW")
    void testNewCard_Rating4_GraduatesImmediately() {
        // Given
        UserCardSrsDO card = createNewCard();
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(4);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(4, updated.getInterval());  // easyInterval
        assertNull(updated.getLapseOldInterval());
    }

    // ========== handleLearningCard() - LEARNING 测试 ==========

    @Test
    @DisplayName("LEARNING卡片 - 评级1(重来) - 重置到step=0")
    void testLearningCard_Rating1_ResetsToStep0() {
        // Given
        UserCardSrsDO card = createLearningCard((byte) 1, 60);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(1);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_LEARNING, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(10, updated.getInterval());  // learningSteps[0]
    }

    @Test
    @DisplayName("LEARNING卡片 - 评级2(困难) 中间步骤 - 延长间隔")
    void testLearningCard_Rating2_MiddleStep_ExtendsInterval() {
        // Given
        UserCardSrsDO card = createLearningCard((byte) 0, 10);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(2);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_LEARNING, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());  // step 不变
        assertEquals(35, updated.getInterval());  // (10 + 60) / 2 = 35
    }

    @Test
    @DisplayName("LEARNING卡片 - 评级2(困难) 最后步骤 - 保持间隔")
    void testLearningCard_Rating2_LastStep_KeepsInterval() {
        // Given
        UserCardSrsDO card = createLearningCard((byte) 1, 60);  // 最后一步
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(2);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_LEARNING, updated.getType());
        assertEquals((byte) 1, updated.getCurrentStep());  // step 不变
        assertEquals(60, updated.getInterval());  // 保持不变
    }

    @Test
    @DisplayName("LEARNING卡片 - 评级3(良好) - 推进到下一步")
    void testLearningCard_Rating3_AdvancesToNextStep() {
        // Given
        UserCardSrsDO card = createLearningCard((byte) 0, 10);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(3);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_LEARNING, updated.getType());
        assertEquals((byte) 1, updated.getCurrentStep());  // step++
        assertEquals(60, updated.getInterval());  // learningSteps[1]
    }

    @Test
    @DisplayName("LEARNING卡片 - 评级3(良好) 最后步骤 - 毕业")
    void testLearningCard_Rating3_LastStep_Graduates() {
        // Given
        UserCardSrsDO card = createLearningCard((byte) 1, 60);  // 最后一步
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(3);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(1, updated.getInterval());  // graduatingInterval
        assertNull(updated.getLapseOldInterval());
    }

    @Test
    @DisplayName("LEARNING卡片 - 评级4(简单) - 立即毕业")
    void testLearningCard_Rating4_GraduatesImmediately() {
        // Given
        UserCardSrsDO card = createLearningCard((byte) 0, 10);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(4);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(4, updated.getInterval());  // easyInterval
    }

    // ========== handleReviewCard() 测试 ==========

    @Test
    @DisplayName("REVIEW卡片 - 评级1(重来) - 遗忘进入RELEARNING")
    void testReviewCard_Rating1_EntersRelearning() {
        // Given
        UserCardSrsDO card = createReviewCard(30, new BigDecimal("2.5"), 5, 0);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(1);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_RELEARNING, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(20, updated.getInterval());  // relearningSteps[0]，单位分钟
        assertEquals((short) 30, updated.getLapseOldInterval());  // 保存遗忘前间隔
        assertEquals(0, updated.getRepetitions());  // 重置
        assertEquals(1, updated.getLapseCount());  // 增加1
        assertEquals(new BigDecimal("2.3"), updated.getEaseFactor());  // 2.5 - 0.2
    }

    @Test
    @DisplayName("REVIEW卡片 - 评级2(困难) - 间隔增长放缓")
    void testReviewCard_Rating2_SlowerGrowth() {
        // Given
        UserCardSrsDO card = createReviewCard(10, new BigDecimal("2.5"), 3, 0);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(2);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals(12, updated.getInterval());  // 10 * 1.2 = 12
        assertEquals(4, updated.getRepetitions());  // 增加
        assertEquals(new BigDecimal("2.35"), updated.getEaseFactor());  // 2.5 - 0.15
    }

    @Test
    @DisplayName("REVIEW卡片 - 评级3(良好) - 标准增长")
    void testReviewCard_Rating3_StandardGrowth() {
        // Given
        UserCardSrsDO card = createReviewCard(10, new BigDecimal("2.5"), 3, 0);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(3);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals(25, updated.getInterval());  // 10 * 2.5 = 25
        assertEquals(4, updated.getRepetitions());
        assertEquals(new BigDecimal("2.5"), updated.getEaseFactor());  // 不变
    }

    @Test
    @DisplayName("REVIEW卡片 - 评级4(简单) - 额外奖励")
    void testReviewCard_Rating4_BonusGrowth() {
        // Given
        UserCardSrsDO card = createReviewCard(10, new BigDecimal("2.5"), 3, 0);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(4);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals(32, updated.getInterval());  // 10 * 2.5 * 1.3 = 32.5 -> 32
        assertEquals(4, updated.getRepetitions());
        assertEquals(new BigDecimal("2.65"), updated.getEaseFactor());  // 2.5 + 0.15
    }

    @Test
    @DisplayName("REVIEW卡片 - EF下限保护")
    void testReviewCard_EFMinimumProtection() {
        // Given
        UserCardSrsDO card = createReviewCard(10, new BigDecimal("1.3"), 3, 0);  // 已经是最小值
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(1);  // 遗忘，应该 -0.2

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(new BigDecimal("1.3"), updated.getEaseFactor());  // 不能低于最小值
    }

    // ========== handleLearningCard() - RELEARNING 测试 ==========

    @Test
    @DisplayName("RELEARNING卡片 - 评级1(重来) - 重置但保持lapseOldInterval")
    void testRelearningCard_Rating1_ResetsButKeepsLapseOldInterval() {
        // Given
        UserCardSrsDO card = createRelearningCard((byte) 0, 20, (short) 30);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(1);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_RELEARNING, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(20, updated.getInterval());  // relearningSteps[0]
        assertEquals((short) 30, updated.getLapseOldInterval());  // 保持不变
    }

    @Test
    @DisplayName("RELEARNING卡片 - 评级3(良好) - 重新毕业")
    void testRelearningCard_Rating3_Regraduates() {
        // Given
        UserCardSrsDO card = createRelearningCard((byte) 0, 20, (short) 30);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(3);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals((byte) 0, updated.getCurrentStep());
        assertEquals(15, updated.getInterval());  // MAX(1, floor(30 * 0.5)) = 15
        assertNull(updated.getLapseOldInterval());  // 清空
    }

    @Test
    @DisplayName("RELEARNING卡片 - 评级3(良好) 低间隔保护")
    void testRelearningCard_Rating3_MinimumIntervalProtection() {
        // Given
        UserCardSrsDO card = createRelearningCard((byte) 0, 20, (short) 2);  // 很小的遗忘前间隔
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(3);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals(1, updated.getInterval());  // MAX(1, floor(2 * 0.5)) = MAX(1, 1) = 1
        assertNull(updated.getLapseOldInterval());
    }

    @Test
    @DisplayName("RELEARNING卡片 - 评级4(简单) - 立即重新毕业")
    void testRelearningCard_Rating4_RegraduatesImmediately() {
        // Given
        UserCardSrsDO card = createRelearningCard((byte) 0, 20, (short) 30);
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(4);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals(15, updated.getInterval());  // MAX(4, floor(30 * 0.5)) = MAX(4, 15) = 15
        assertNull(updated.getLapseOldInterval());
    }

    @Test
    @DisplayName("RELEARNING卡片 - 评级4(简单) easyInterval更大时使用easyInterval")
    void testRelearningCard_Rating4_UsesEasyIntervalWhenLarger() {
        // Given
        UserCardSrsDO card = createRelearningCard((byte) 0, 20, (short) 6);  // 较小的遗忘前间隔
        ReviewCardRequest request = new ReviewCardRequest();
        request.setCardId(1L);
        request.setResult(4);

        when(srsStateDataService.getByUserAndCard(anyLong(), anyLong())).thenReturn(card);

        // When
        reviewService.submitReview(1L, request);

        // Then
        ArgumentCaptor<UserCardSrsDO> captor = ArgumentCaptor.forClass(UserCardSrsDO.class);
        verify(srsStateDataService).update(captor.capture());

        UserCardSrsDO updated = captor.getValue();
        assertEquals(TYPE_REVIEW, updated.getType());
        assertEquals(4, updated.getInterval());  // MAX(4, floor(6 * 0.5)) = MAX(4, 3) = 4
        assertNull(updated.getLapseOldInterval());
    }

    // ========== 辅助方法 ==========

    private UserCardSrsDO createNewCard() {
        UserCardSrsDO card = new UserCardSrsDO();
        card.setId(1L);
        card.setUserId(1L);
        card.setCardId(1L);
        card.setType(TYPE_NEW);
        card.setCurrentStep((byte) 0);
        card.setInterval(0);
        card.setEaseFactor(new BigDecimal("2.5"));
        card.setRepetitions(0);
        card.setLapseCount(0);
        card.setLapseOldInterval(null);
        return card;
    }

    private UserCardSrsDO createLearningCard(byte currentStep, int interval) {
        UserCardSrsDO card = new UserCardSrsDO();
        card.setId(1L);
        card.setUserId(1L);
        card.setCardId(1L);
        card.setType(TYPE_LEARNING);
        card.setCurrentStep(currentStep);
        card.setInterval(interval);
        card.setEaseFactor(new BigDecimal("2.5"));
        card.setRepetitions(0);
        card.setLapseCount(0);
        card.setLapseOldInterval(null);
        return card;
    }

    private UserCardSrsDO createReviewCard(int interval, BigDecimal easeFactor, int repetitions, int lapseCount) {
        UserCardSrsDO card = new UserCardSrsDO();
        card.setId(1L);
        card.setUserId(1L);
        card.setCardId(1L);
        card.setType(TYPE_REVIEW);
        card.setCurrentStep((byte) 0);
        card.setInterval(interval);
        card.setEaseFactor(easeFactor);
        card.setRepetitions(repetitions);
        card.setLapseCount(lapseCount);
        card.setLapseOldInterval(null);
        return card;
    }

    private UserCardSrsDO createRelearningCard(byte currentStep, int interval, short lapseOldInterval) {
        UserCardSrsDO card = new UserCardSrsDO();
        card.setId(1L);
        card.setUserId(1L);
        card.setCardId(1L);
        card.setType(TYPE_RELEARNING);
        card.setCurrentStep(currentStep);
        card.setInterval(interval);
        card.setEaseFactor(new BigDecimal("2.3"));  // 遗忘后降低的EF
        card.setRepetitions(0);
        card.setLapseCount(1);  // 至少遗忘过一次
        card.setLapseOldInterval(lapseOldInterval);
        return card;
    }
}
