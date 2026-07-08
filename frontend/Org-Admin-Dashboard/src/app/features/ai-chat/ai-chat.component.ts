import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { AI_CHAT_STRINGS } from './ai-chat.strings';
import { ChatMessage } from '../../core/models/ai-chat.model';
import { AiChatService } from '../../core/services/ai-chat.service';
import { LinkifyDirective } from '../../core/directives/linkify.directive';

/**
 * AiChatComponent — stands up the chat assistant UI workspace, connecting the
 * layout views to the AiChatService stream and linkify directive.
 */
@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LinkifyDirective,
  ],
  templateUrl: './ai-chat.component.html',
  styleUrls: ['./ai-chat.component.scss'],
})
export class AiChatComponent implements OnInit, OnDestroy, AfterViewChecked {
  readonly strings = AI_CHAT_STRINGS;

  messages: ChatMessage[] = [];
  isAiTyping = false;
  userInput = '';

  private readonly subscription = new Subscription();

  @ViewChild('scrollContainer') private readonly scrollContainer!: ElementRef;

  constructor(private readonly aiChatService: AiChatService) {}

  ngOnInit(): void {
    this.aiChatService.loadHistory();

    // Listen for incoming messages
    this.subscription.add(
      this.aiChatService.getMessages().subscribe({
        next: (msgs) => {
          this.messages = msgs;
        },
        error: (err) => console.error('Error fetching chat messages:', err),
      })
    );

    // Listen for AI typing status
    this.subscription.add(
      this.aiChatService.isAiTyping$.subscribe({
        next: (typing) => {
          this.isAiTyping = typing;
        },
        error: (err) => console.error('Error fetching typing state:', err),
      })
    );
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Dispatches the current value of the user input box.
   */
  sendCurrentMessage(): void {
    const text = this.userInput.trim();
    if (!text || this.isAiTyping) return;

    this.aiChatService.sendMessage(text);
    this.userInput = '';
  }

  /**
   * Dispatches a selected suggestion prompt immediately.
   */
  selectSuggestion(suggest: string): void {
    if (this.isAiTyping) return;
    this.aiChatService.sendMessage(suggest);
  }

  /**
   * Intercepts Enter keystrokes to trigger submission, allowing newlines with Shift+Enter.
   */
  handleKeyDown(event: Event): void {
    const keyEvent = event as KeyboardEvent;
    if (keyEvent.key === 'Enter' && !keyEvent.shiftKey) {
      keyEvent.preventDefault();
      this.sendCurrentMessage();
    }
  }

  /**
   * Scrolls the conversation container to the absolute bottom to focus on fresh elements.
   */
  private scrollToBottom(): void {
    try {
      const el = this.scrollContainer.nativeElement;
      el.scrollTop = el.scrollHeight;
    } catch (err) {
      // Container not initialized yet
    }
  }
}
