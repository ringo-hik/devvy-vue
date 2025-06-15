<template>
  <div class="feedback-tab">
    <div v-if="successMessage" class="feedback-result success">
      <div class="result-icon">🎉</div>
      <h4>{{ getText('thankYou') }}</h4>
      <p>{{ successMessage }}</p>
      <button @click="resetForm">{{ getText('sendMore') }}</button>
    </div>
    <div v-else class="feedback-form">
      <div class="feedback-header">
        <h3>{{ getText('title') }}</h3>
        <p>{{ getText('subtitle') }}</p>
      </div>
      
      <div v-if="errorMessage" class="feedback-result error">
        <p>⚠️ {{ errorMessage }}</p>
      </div>
      
      <div class="form-group">
        <label>{{ getText('ratingLabel') }} <span class="required">*</span></label>
        <div class="rating-stars">
          <button v-for="star in 5" :key="star" @click="feedback.rating = star" :class="{ 'active': star <= feedback.rating }">
            ⭐
          </button>
        </div>
      </div>
      
      <div class="form-group">
        <label>{{ getText('contentLabel') }} <span class="required">*</span></label>
        <textarea v-model="feedback.comment" :placeholder="getText('contentPlaceholder')" maxlength="1000"></textarea>
        <div class="char-count">{{ feedback.comment.length }}/1000</div>
      </div>
      
      <div class="form-group">
        <label>{{ getText('categoryLabel') }}</label>
        <div class="category-pills">
            <button v-for="cat in categories" :key="cat" @click="feedback.feedbackCategory = cat" :class="{ active: feedback.feedbackCategory === cat }">
              {{ getText(`categories.${cat}`) }}
            </button>
        </div>
      </div>
      
      <button class="submit-btn" :disabled="!canSubmit" @click="submitFeedback">
        {{ isSubmitting ? getText('submitting') : getText('submit') }}
      </button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FeedbackTab',
  props: ['currentLanguage'],
  data() {
    return {
      isSubmitting: false,
      successMessage: '',
      errorMessage: '',
      feedback: {
        rating: 0,
        comment: '',
        feedbackCategory: ''
      },
      categories: ['ui_ux', 'performance', 'functionality', 'other'],
    };
  },
  computed: {
    texts() {
      const allTexts = {
        ko: {
          title: '피드백 보내기', 
          subtitle: '여러분의 소중한 의견으로 Devvy Bot이 발전합니다.',
          ratingLabel: '만족도', 
          contentLabel: '의견', 
          categoryLabel: '분야',
          contentPlaceholder: '개선점, 칭찬, 버그 리포트 등 무엇이든 좋습니다.',
          submit: '제출하기', 
          submitting: '제출 중...', 
          thankYou: '피드백 감사합니다!',
          sendMore: '피드백 더 보내기',
          categories: { 
            ui_ux: 'UI/UX', 
            performance: '성능', 
            functionality: '기능 정확도', 
            other: '기타' 
          }
        },
        en: {
          title: 'Send Feedback', 
          subtitle: 'Your feedback helps Devvy Bot improve.',
          ratingLabel: 'Satisfaction', 
          contentLabel: 'Comment', 
          categoryLabel: 'Category',
          contentPlaceholder: 'Any suggestions, praise, or bug reports are welcome.',
          submit: 'Submit', 
          submitting: 'Submitting...', 
          thankYou: 'Thank you for your feedback!',
          sendMore: 'Send More Feedback',
          categories: { 
            ui_ux: 'UI/UX', 
            performance: 'Performance', 
            functionality: 'Accuracy', 
            other: 'Other' 
          }
        }
      };
      return allTexts[this.currentLanguage];
    },
    canSubmit() {
      return this.feedback.rating > 0 && 
             this.feedback.comment.trim().length > 5 && 
             !this.isSubmitting;
    }
  },
  methods: {
    getText(key) { 
      return this.texts[key] || key; 
    },
    
    resetForm() {
      this.feedback = { rating: 0, comment: '', feedbackCategory: '' };
      this.successMessage = '';
      this.errorMessage = '';
      this.isSubmitting = false;
    },
    
    submitFeedback() {
      if (!this.canSubmit) return;
      this.isSubmitting = true;
      this.errorMessage = '';
      this.$emit('feedback-sent', this.feedback);
    },
    
    showSuccess(message) {
      this.isSubmitting = false;
      this.successMessage = message;
    },
    
    showError(message) {
      this.isSubmitting = false;
      this.errorMessage = message;
    }
  }
};
</script>