<template>
  <div class="feedback-tab">
    <!-- 결과 표시 영역 -->
    <div v-if="resultMessage" class="result" :class="resultType">
      <template v-if="resultType === 'success'">
        <unicon name="check-circle" fill="#38B2AC" :width="24" :height="24"></unicon>
        <div>
          <h4>{{ getText('feedbackSuccess') }}</h4>
          <p>{{ resultMessage }}</p>
        </div>
        <button @click="resetForm" class="action-btn">
          <unicon name="plus" fill="#FFFFFF" :width="16" :height="16"></unicon>
          {{ getText('sendAnother') }}
        </button>
      </template>
      
      <template v-if="resultType === 'error'">
        <unicon name="exclamation-triangle" fill="#E53E3E" :width="20" :height="20"></unicon>
        <span>{{ resultMessage }}</span>
      </template>
    </div>

    <!-- 피드백 폼 -->
    <div v-if="!resultMessage || resultType === 'error'" class="feedback-form">
      <!-- 폼 헤더 -->
      <div class="form-header">
        <unicon name="heart" fill="#2C5AA0" :width="36" :height="36"></unicon>
        <h3>{{ getText('feedbackTitle') }}</h3>
        <p>{{ getText('feedbackDescription') }}</p>
      </div>

      <!-- 피드백 콘텐츠 (스크롤 가능) -->
      <div class="feedback-content">
        <!-- 평점 -->
        <div class="group">
          <label>
            <unicon name="star" fill="#2C5AA0" :width="16" :height="16"></unicon>
            {{ getText('rating') }}
            <span class="required">*</span>
          </label>
          <div class="rating-stars">
            <button v-for="star in 5" 
                    :key="star"
                    @click="setRating(star)"
                    :class="['star', { active: star <= selectedRating, hover: star <= hoverRating }]"
                    @mouseenter="hoverRating = star"
                    @mouseleave="hoverRating = 0"
                    type="button">
              <unicon name="star" :fill="(star <= selectedRating || star <= hoverRating) ? '#FFD700' : '#E0E0E0'" :width="24" :height="24"></unicon>
            </button>
          </div>
          <div class="rating-text">
            {{ getRatingText(selectedRating || hoverRating) }}
          </div>
        </div>

        <!-- 피드백 카테고리 -->
        <div class="group">
          <label>
            <unicon name="tag" fill="#2C5AA0" :width="16" :height="16"></unicon>
            {{ getText('feedbackCategory') }}
          </label>
          <div class="categories">
            <button v-for="category in feedbackCategories" 
                    :key="category.value"
                    @click="toggleCategory(category.value)"
                    :class="['category', { active: selectedCategories.includes(category.value) }]"
                    type="button">
              <unicon :name="category.icon" :fill="selectedCategories.includes(category.value) ? '#FFFFFF' : '#2C5AA0'" :width="14" :height="14"></unicon>
              {{ category.label }}
            </button>
          </div>
        </div>

        <!-- 상세 의견 -->
        <div class="group">
          <label>
            <unicon name="edit" fill="#2C5AA0" :width="16" :height="16"></unicon>
            {{ getText('detailedComment') }}
          </label>
          <textarea v-model="comment" 
                    :placeholder="getText('commentPlaceholder')"
                    maxlength="1000"
                    @input="updateCharCount"></textarea>
          <div class="char-count">
            {{ comment.length }} / 1000
          </div>
        </div>
      </div>

      <!-- 제출 버튼 -->
      <button @click="submitFeedback" 
              :disabled="!isFormValid || isSubmitting" 
              class="submit">
        <template v-if="isSubmitting">
          <div class="spinner"></div>
          {{ getText('submitting') }}
        </template>
        <template v-else>
          <unicon name="message" fill="#FFFFFF" :width="16" :height="16"></unicon>
          {{ getText('submitFeedback') }}
        </template>
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FeedbackTab',
  props: {
    currentLanguage: {
      type: String,
      default: 'ko'
    }
  },
  data() {
    return {
      selectedRating: 0,
      hoverRating: 0,
      selectedCategories: [],
      comment: '',
      isSubmitting: false,
      resultMessage: '',
      resultType: '', // 'success' or 'error'
    };
  },
  computed: {
    texts() {
      return {
        ko: {
          feedbackTitle: '피드백 보내기',
          feedbackDescription: '서비스 개선을 위해 소중한 의견을 들려주세요.',
          rating: '전체적인 만족도',
          feedbackCategory: '피드백 유형',
          detailedComment: '상세 의견',
          commentPlaceholder: '서비스에 대한 자세한 의견이나 개선사항을 알려주세요...',
          submitFeedback: '피드백 보내기',
          submitting: '전송 중...',
          feedbackSuccess: '피드백 전송 완료',
          sendAnother: '다른 피드백 보내기',
          // 평점 텍스트
          ratingTexts: ['평점을 선택해주세요', '매우 불만족', '불만족', '보통', '만족', '매우 만족'],
          // 피드백 카테고리
          categories: {
            usability: '사용성',
            performance: '성능',
            content: '콘텐츠',
            design: '디자인',
            bug: '버그 신고',
            suggestion: '개선 제안'
          }
        },
        en: {
          feedbackTitle: 'Send Feedback',
          feedbackDescription: 'Please share your valuable opinions to help us improve our service.',
          rating: 'Overall Satisfaction',
          feedbackCategory: 'Feedback Type',
          detailedComment: 'Detailed Comments',
          commentPlaceholder: 'Please tell us your detailed opinions or suggestions for improvement...',
          submitFeedback: 'Send Feedback',
          submitting: 'Submitting...',
          feedbackSuccess: 'Feedback Sent Successfully',
          sendAnother: 'Send Another Feedback',
          // 평점 텍스트
          ratingTexts: ['Please select a rating', 'Very Dissatisfied', 'Dissatisfied', 'Neutral', 'Satisfied', 'Very Satisfied'],
          // 피드백 카테고리
          categories: {
            usability: 'Usability',
            performance: 'Performance',
            content: 'Content',
            design: 'Design',
            bug: 'Bug Report',
            suggestion: 'Suggestion'
          }
        }
      }[this.currentLanguage];
    },
    
    feedbackCategories() {
      return [
        { value: 'usability', label: this.texts.categories.usability, icon: 'mouse' },
        { value: 'performance', label: this.texts.categories.performance, icon: 'rocket' },
        { value: 'content', label: this.texts.categories.content, icon: 'document' },
        { value: 'design', label: this.texts.categories.design, icon: 'palette' },
        { value: 'bug', label: this.texts.categories.bug, icon: 'bug' },
        { value: 'suggestion', label: this.texts.categories.suggestion, icon: 'lightbulb' }
      ];
    },
    
    isFormValid() {
      return this.selectedRating > 0;
    }
  },
  methods: {
    getText(key) {
      return this.texts[key] || key;
    },
    
    setRating(rating) {
      this.selectedRating = rating;
      this.clearResult();
    },
    
    toggleCategory(categoryValue) {
      const index = this.selectedCategories.indexOf(categoryValue);
      if (index > -1) {
        this.selectedCategories.splice(index, 1);
      } else {
        this.selectedCategories.push(categoryValue);
      }
      this.clearResult();
    },
    
    getRatingText(rating) {
      if (!rating || rating < 1 || rating > 5) {
        return this.texts.ratingTexts[0];
      }
      return this.texts.ratingTexts[rating];
    },
    
    updateCharCount() {
      this.clearResult();
    },
    
    clearResult() {
      if (this.resultMessage && this.resultType === 'error') {
        this.resultMessage = '';
        this.resultType = '';
      }
    },
    
    submitFeedback() {
      if (!this.isFormValid || this.isSubmitting) return;
      
      this.isSubmitting = true;
      this.resultMessage = '';
      this.resultType = '';
      
      const feedbackData = {
        rating: this.selectedRating,
        feedbackCategory: this.selectedCategories.join(','),
        comment: this.comment.trim()
      };
      
      // 부모 컴포넌트로 피드백 데이터 전송
      this.$emit('feedback-sent', feedbackData);
    },
    
    showSuccess(message) {
      this.isSubmitting = false;
      this.resultMessage = message || this.getText('feedbackSuccess');
      this.resultType = 'success';
    },
    
    showError(message) {
      this.isSubmitting = false;
      this.resultMessage = message || 'An error occurred while sending feedback.';
      this.resultType = 'error';
    },
    
    resetForm() {
      this.selectedRating = 0;
      this.hoverRating = 0;
      this.selectedCategories = [];
      this.comment = '';
      this.isSubmitting = false;
      this.resultMessage = '';
      this.resultType = '';
    }
  }
};
</script>

<style scoped>
/* 피드백 탭 전체 레이아웃 */
.feedback-tab {
  padding: 16px;
  overflow-y: auto;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 폼 헤더 */
.form-header {
  text-align: center;
  margin-bottom: 24px;
}

.form-header h3 {
  font-size: 18px;
  margin: 12px 0 8px;
  color: var(--text-dark);
  font-weight: 600;
}

.form-header p {
  color: var(--text-light);
  margin: 0;
  line-height: 1.5;
  font-size: 13px;
}

/* 피드백 콘텐츠 (스크롤 가능) */
.feedback-content {
  overflow-y: auto;
  flex: 1;
  margin-bottom: 16px;
}

/* 그룹 스타일 */
.group {
  margin-bottom: 20px;
}

.group label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
  margin-bottom: 8px;
  color: var(--text-medium);
  font-size: 14px;
}

.group label .required {
  color: var(--error);
  margin-left: 2px;
}

/* 평점 스타일 */
.rating-stars {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
}

.rating-stars .star {
  background: none;
  border: none;
  cursor: pointer;
  transition: transform 0.2s;
  padding: 2px;
  border-radius: 4px;
}

.rating-stars .star:hover {
  transform: scale(1.1);
}

.rating-text {
  font-size: 12px;
  color: var(--text-light);
  font-weight: 500;
  min-height: 16px;
}

/* 카테고리 스타일 */
.categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.categories .category {
  background: #e9ecef;
  border: none;
  border-radius: 20px;
  padding: 6px 12px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-medium);
  display: flex;
  align-items: center;
  gap: 4px;
}

.categories .category:hover {
  background: #dee2e6;
}

.categories .category.active {
  background: var(--primary);
  color: white;
}

/* 텍스트 영역 */
.group textarea {
  width: 100%;
  min-height: 100px;
  resize: vertical;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  padding: 12px;
  font-family: inherit;
  font-size: 14px;
  background: white;
  transition: border-color 0.2s;
  line-height: 1.5;
  color: var(--text-dark);
}

.group textarea:focus {
  border-color: var(--primary);
  outline: none;
}

.group textarea::placeholder {
  color: var(--text-light);
}

.char-count {
  text-align: right;
  font-size: 11px;
  color: var(--text-light);
  margin-top: 4px;
}

/* 제출 버튼 */
.submit {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  background: var(--primary);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 16px;
  margin-bottom: 16px;
}

.submit:hover:not(:disabled) {
  background: var(--primary-dark);
}

.submit:disabled {
  background: #ced4da;
  cursor: not-allowed;
}

.submit .spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

/* 결과 표시 */
.result {
  text-align: center;
  padding: 30px 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.result.success {
  background: rgba(56, 178, 172, 0.1);
  border: 1px solid rgba(56, 178, 172, 0.2);
  color: var(--success);
}

.result.error {
  background: rgba(229, 62, 62, 0.1);
  border: 1px solid rgba(229, 62, 62, 0.2);
  color: var(--error);
  flex-direction: row;
  text-align: left;
  padding: 12px 16px;
}

.result h4 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-dark);
}

.result p {
  margin: 0;
  font-size: 14px;
  color: var(--text-medium);
  line-height: 1.4;
}

.result .action-btn {
  background: var(--primary);
  color: white;
  border: none;
  border-radius: 6px;
  padding: 10px 20px;
  cursor: pointer;
  transition: background 0.2s;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
}

.result .action-btn:hover {
  background: var(--primary-dark);
}

/* 스크롤바 */
.feedback-content::-webkit-scrollbar {
  width: 4px;
}

.feedback-content::-webkit-scrollbar-track {
  background: transparent;
}

.feedback-content::-webkit-scrollbar-thumb {
  background: #ced4da;
  border-radius: 2px;
}

/* 애니메이션 */
@keyframes spin {
  100% { transform: rotate(360deg); }
}

/* 반응형 */
@media (max-width: 480px) {
  .categories {
    flex-direction: column;
    gap: 6px;
  }
  
  .categories .category {
    justify-content: center;
  }
  
  .rating-stars {
    justify-content: center;
  }
}
</style>