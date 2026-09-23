import { Component, EventEmitter, HostListener, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { User, UserRole } from '../../models';

@Component({
  selector: 'app-user-form-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-form-modal.component.html'
})
export class UserFormModalComponent {
  @Input() user: User | null = null;
  @Output() closed = new EventEmitter<void>();
  @Output() saved = new EventEmitter<{ name: string; email: string; role: string; password?: string }>();

  readonly roles: UserRole[] = ['USER', 'ADMIN'];
  readonly validationError = signal<string | null>(null);

  @HostListener('window:keydown.Escape')
  onEscKey() {
    this.close();
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.close();
    }
  }

  close(): void {
    this.validationError.set(null);
    this.closed.emit();
  }

  save(name: string, email: string, role: string, password?: string): void {
    if (!name.trim() || !email.trim()) {
      this.validationError.set('Nombre y correo son obligatorios.');
      return;
    }
    this.validationError.set(null);
    this.saved.emit({ name, email, role, password });
  }
}
