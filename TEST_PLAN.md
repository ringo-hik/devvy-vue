# Test Plan for Devvy Bot Refinements

This document outlines the test plan for verifying the recent UI/UX refinements and backend consistency checks for the Devvy Bot application.

**I. Frontend UI/UX Testing (Manual Browser Testing)**

1.  **Floating Button & Window:**
    *   **TC1.1**: Verify the floating button (`🤖`) is visible at the bottom right of the application page.
    *   **TC1.2**: Verify the hover animation on the floating button is subtle (expected: `transform: translateY(-2px) scale(1.02);`).
    *   **TC1.3**: Click the floating button: Verify the Devvy Bot window opens smoothly using the defined fade/scale animation (expected: `cubic-bezier(0.25, 0.1, 0.25, 1)`).
    *   **TC1.4**: In the Devvy Bot window header, verify the bot avatar (currently `🤖`), the name "Devvy Bot", and the online/offline status indicator are correctly displayed.
    *   **TC1.5**: Verify that the header action buttons (Language, Minimize, Close) use the new SVG icons and exhibit a minimal color tone.
        *   Language icon should be `language-icon.svg` (or equivalent).
        *   Minimize icon should be `minimize-icon.svg` (or equivalent).
        *   Close icon should be `close-icon.svg` (or equivalent).
    *   **TC1.6**: Hover over each header action button: Verify a subtle visual feedback (e.g., background change).
    *   **TC1.7**: Click the Minimize button: Verify the window closes with the defined animation. Re-click the floating button to ensure the window reopens.
    *   **TC1.8**: Click the Close button: Verify the window closes with the defined animation. Re-click the floating button to ensure the window reopens (note: state might be reset as per `closeBot()` implementation).

2.  **Tabs & Navigation:**
    *   **TC2.1**: Verify that the tab menu buttons (Chat, History, Feedback) display the new SVG icons (`chat-icon.svg`, `history-icon.svg`, `feedback-icon.svg` or equivalents) with a minimal, muted color tone when inactive.
    *   **TC2.2**: Verify that the active tab has a distinct visual style (e.g., accent color line underneath, icon fully colored/less muted).
    *   **TC2.3**: Click on each tab: Verify smooth transition between tab content areas and that the correct content for each tab is displayed.

3.  **Chat Tab (`ChatTab.vue`):**
    *   **TC3.1**: **Category Display**: When no category is selected, verify that categories are displayed in a single, horizontally scrollable row.
    *   **TC3.2**: Verify that category cards in the list use the default SVG icon (`cat-icon.svg` or equivalent) and that the icon has a minimal color tone.
    *   **TC3.3**: Hover over a category card: Verify the hover effect is subtle (expected: `transform: translateY(-2px); box-shadow: 0 3px 8px rgba(0,0,0,0.04); border-color: $c-primary-light;`).
    *   **TC3.4**: Click on a category card: Verify that the view transitions to the chat input screen for the selected category.
    *   **TC3.5**: **Message Input**: Verify the send button in the input area uses the new `send-icon.svg` (or equivalent) and is styled correctly (e.g., white icon on accent color background).
    *   **TC3.6**: Type and send a message: Verify the user's message appears correctly styled (e.g., on the right side, with a primary color background).
    *   **TC3.7**: (Requires backend interaction) Verify an AI response appears correctly styled (e.g., on the left side, with an alternative background color).
    *   **TC3.8**: (Requires backend interaction) Verify that appropriate loading messages/indicators are displayed while waiting for an AI response.

4.  **History Tab (`HistoryTab.vue`):**
    *   **TC4.1**: If chat history exists, verify it is displayed clearly, potentially grouped by date.
    *   **TC4.2**: Verify functionality for viewing details of a past conversation by clicking on a history item.

5.  **Feedback Tab (`FeedbackTab.vue`):**
    *   **TC5.1**: Verify the feedback form, including rating stars and comment box, is correctly displayed.
    *   **TC5.2**: Test interactivity: Click rating stars, enter text into the comment box. (Submission testing is covered under backend).

6.  **Responsiveness & Styling:**
    *   **TC6.1**: If scrollbars are visible in any scrollable area (e.g., category list, message list), verify they are minimally styled as per `devvy.scss` updates.
    *   **TC6.2**: Briefly check the chatbot window at a typical desktop resolution. Ensure no major layout breaks or overlapping elements. (Full responsiveness testing across multiple devices is likely out of scope for these specific refinements).

**II. Backend Integration & Service Testing (Conceptual - requires running services and tools like browser developer console)**

1.  **Service Replacement & API Calls (`devvyService.js-real`):**
    *   **TC7.1**: During UI testing, use browser developer tools (Network tab) to confirm that API calls (e.g., for categories, sending messages, fetching history) are directed to the actual backend endpoints (e.g., `/api/v1/devportal/devvy/...`) via `devvyService.js-real.js`.

2.  **Java Backend (`DevvyService.java`, `devvyMapper.xml`):**
    *   **TC8.1**: **Category Loading**: Trigger category loading in the frontend. Verify that `DevvyService.getCategories()` is called, and it successfully fetches data from the `devvy_category` table via `devvyMapper.selectCategories`. Ensure these categories are correctly displayed in the UI.
    *   **TC8.2**: **Chat Processing**:
        *   Send a chat message from the frontend.
        *   Verify that `DevvyService.processDevvyChat()` is invoked.
        *   Confirm that the user's message is saved to the `devvy_message` table (via `insertChatMessage`).
        *   Confirm that an AI response (even if using the dummy logic in `generateDummyAiResponse` for now) is generated and saved to `devvy_message`.
        *   Confirm that a new session is created in `devvy_session` (via `insertChatSession`) or an existing one is updated (via `updateChatSession`, checking `message_count` and `last_message_at`).
        *   Verify the AI response is correctly returned to and displayed on the frontend.
    *   **TC8.3**: **History Functionality**:
        *   Access the History tab on the frontend.
        *   Verify `DevvyService.getChatHistory()` is called and correctly retrieves data via `devvyMapper.selectChatHistory`.
        *   When viewing a specific session's messages, verify `DevvyService.getSessionMessages()` is called and retrieves data via `devvyMapper.selectSessionMessages`.
    *   **TC8.4**: **Feedback Submission**:
        *   Submit feedback from the Feedback tab.
        *   Verify `DevvyService.saveFeedback()` is invoked and data is stored in the `devvy_feedback` table via `devvyMapper.insertFeedback`.
    *   **TC8.5**: **Database Consistency**: After performing various chat and feedback operations, manually inspect the relevant tables (`devvy_category`, `devvy_message`, `devvy_session`, `devvy_feedback`) to ensure data integrity, correct foreign key relationships, and accurate timestamping.

**III. General Checks**

1.  **Language Toggle:**
    *   **TC9.1**: Click the language toggle button in the Devvy Bot header. Verify that all relevant UI text elements (e.g., tab names, button titles/tooltips, status messages, dynamic text from `texts` computed property) switch correctly between Korean and English.
    *   **TC9.2**: (If applicable) Verify if the language preference is persisted across sessions (e.g., if `this.$devvy.setLanguage` uses localStorage or similar).

2.  **Error Handling:**
    *   **TC10.1**: If possible to simulate a backend API error (e.g., by temporarily stopping the backend service or using browser tools to block a request), verify that a user-friendly error message is displayed in the chat interface (e.g., the "networkError" message from the i18n bundle).

3.  **Console Errors:**
    *   **TC11.1**: Throughout all manual testing, keep the browser's developer console open. Monitor for any new JavaScript errors or warnings that might be generated by the chatbot's operations. Address any identified issues.

This test plan serves as a guide for ensuring the quality and correctness of the implemented refinements.
