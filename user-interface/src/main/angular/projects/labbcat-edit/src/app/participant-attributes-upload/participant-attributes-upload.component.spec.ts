import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ParticipantAttributesUploadComponent } from './participants-upload.component';

describe('ParticipantAttributesUploadComponent', () => {
  let component: ParticipantAttributesUploadComponent;
  let fixture: ComponentFixture<ParticipantAttributesUploadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ParticipantAttributesUploadComponent]
    })
    .compileComponents();
    
    fixture = TestBed.createComponent(ParticipantAttributesUploadComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
