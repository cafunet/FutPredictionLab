import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-stat-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './stat-card.component.html',
  styleUrl: './stat-card.component.css'
})
export class StatCardComponent {
  @Input({ required: true }) value!: string | number;
  @Input({ required: true }) label!: string;
  @Input() color: 'primary' | 'success' | 'danger' = 'primary';

  get colorClass(): string {
    switch (this.color) {
      case 'success': return 'text-emerald-600';
      case 'danger': return 'text-red-500';
      default: return 'text-[#063b27]';
    }
  }
}
