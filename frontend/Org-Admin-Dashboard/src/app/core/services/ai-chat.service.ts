import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { ChatMessage } from '../models/ai-chat.model';
import { environment } from '../../../environments/environment';

/**
 * AiChatService — manages the conversation thread state for the AI Assistant.
 * Messages are sent to the real backend AI endpoint; no simulated responses.
 */
@Injectable({
  providedIn: 'root',
})
export class AiChatService {
  private readonly chatUrl = `${environment.apiBaseUrl}/api/ai/chat`;

  private readonly messagesSubject = new BehaviorSubject<ChatMessage[]>([]);
  readonly messages$ = this.messagesSubject.asObservable();

  private readonly isAiTypingSubject = new BehaviorSubject<boolean>(false);
  readonly isAiTyping$ = this.isAiTypingSubject.asObservable();

  constructor(private readonly http: HttpClient) {
    this.initializeWelcomeMessage();
  }

  private initializeWelcomeMessage(): void {
    const welcomeMessage: ChatMessage = {
      id: 'msg-welcome',
      sender: 'ai',
      text: "Hello! I'm Praja Disha AI. I have access to your latest surveys, community feedback, and infrastructure reports.\n\nHow can I help you analyze your local government data today? You can ask me things like:",
      timestamp: Date.now(),
      suggestions: [
        'Summarize the main concerns from the Q3 town hall survey.',
        'Show me the trend in road maintenance requests over the last 6 months.',
        'Compare resident satisfaction rates between District A and District B.',
      ],
    };
    this.messagesSubject.next([welcomeMessage]);
  }

  /**
   * Returns the stream of messages.
   */
  getMessages(): Observable<ChatMessage[]> {
    return this.messages$;
  }

  /**
   * Sends a user message to the backend AI and appends the response to the thread.
   */
  sendMessage(text: string): void {
    if (!text.trim()) return;

    // 1. Append the user's message immediately
    const userMsg: ChatMessage = {
      id: `msg-user-${Date.now()}`,
      sender: 'user',
      text: text.trim(),
      timestamp: Date.now(),
    };
    this.messagesSubject.next([...this.messagesSubject.value, userMsg]);

    // 2. Show typing indicator while awaiting the backend response
    this.isAiTypingSubject.next(true);

    // 3. Send to the backend and append the AI reply
    this.http.post<ChatMessage>(this.chatUrl, { text: text.trim() }).pipe(
      tap(() => this.isAiTypingSubject.next(false))
    ).subscribe({
      next: (aiResponse) => {
        this.messagesSubject.next([...this.messagesSubject.value, aiResponse]);
      },
      error: () => {
        // On error, hide typing indicator and show a fallback message
        this.isAiTypingSubject.next(false);
        const errorMsg: ChatMessage = {
          id: `msg-err-${Date.now()}`,
          sender: 'ai',
          text: 'I encountered an error processing your request. Please try again.',
          timestamp: Date.now(),
        };
        this.messagesSubject.next([...this.messagesSubject.value, errorMsg]);
      }
    });
  }
}
