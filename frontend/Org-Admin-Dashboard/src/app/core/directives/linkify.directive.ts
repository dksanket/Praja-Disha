import { Directive, ElementRef, Input, OnChanges } from '@angular/core';

/**
 * LinkifyDirective — Automatically parses raw URLs and markdown-style links [label](url)
 * in text and converts them into clickable secure anchor tags with the class `chat-message__link`.
 * Also escapes HTML inputs to prevent cross-site scripting (XSS).
 */
@Directive({
  selector: '[appLinkify]',
  standalone: true,
})
export class LinkifyDirective implements OnChanges {
  @Input() appLinkify = '';

  constructor(private el: ElementRef) {}

  ngOnChanges(): void {
    let text = this.appLinkify || '';

    // Escape HTML characters first to prevent raw script execution
    text = this.escapeHtml(text);

    // Matches markdown link [label](url) in group 1, label in group 2, url in group 3.
    // Matches raw HTTP/S URLs in group 4.
    const regex = /(\[([^\]]+)\]\((https?:\/\/[^\s)]+)\))|(https?:\/\/[^\s]+)/g;

    const html = text.replace(regex, (match, p1, p2, p3, p4) => {
      if (p1) {
        // Render Markdown link
        return `<a href="${p3}" target="_blank" rel="noopener noreferrer" class="chat-message__link">${p2}</a>`;
      } else {
        // Render Raw URL
        return `<a href="${p4}" target="_blank" rel="noopener noreferrer" class="chat-message__link">${p4}</a>`;
      }
    });

    this.el.nativeElement.innerHTML = html;
  }

  private escapeHtml(unsafe: string): string {
    const map: Record<string, string> = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      '"': '&quot;',
      "'": '&#039;',
    };
    return unsafe.replace(/[&<>"']/g, (m) => map[m]);
  }
}
