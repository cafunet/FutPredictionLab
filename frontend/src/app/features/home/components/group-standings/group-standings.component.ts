import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Standing } from '../../../../shared/models';

@Component({
  selector: 'app-group-standings',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './group-standings.component.html',
  styleUrl: './group-standings.component.css'
})
export class GroupStandingsComponent {
  @Input({ required: true }) group!: string;
  @Input({ required: true }) standings!: Standing[];
  @Input() liveUpdating = false;
  @Output() teamClicked = new EventEmitter<Standing>();

  onTeamClick(standing: Standing): void {
    this.teamClicked.emit(standing);
  }
}
