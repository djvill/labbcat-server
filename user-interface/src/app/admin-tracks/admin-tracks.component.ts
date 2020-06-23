import { Component, OnInit } from '@angular/core';

import { Response } from '../response';
import { MediaTrack } from '../media-track';
import { MessageService } from '../message.service';
import { LabbcatService } from '../labbcat.service';

@Component({
  selector: 'app-admin-tracks',
  templateUrl: './admin-tracks.component.html',
  styleUrls: ['./admin-tracks.component.css']
})
export class AdminTracksComponent implements OnInit {
    rows: MediaTrack[];
    changed = false;
    constructor(
        private labbcatService: LabbcatService,
        private messageService: MessageService
    ) { }
    
    ngOnInit(): void {
        this.readRows();
    }

    readRows(): void {
        this.labbcatService.labbcat.readMediaTracks((tracks, errors, messages) => {
            this.rows = [];
            for (let track of tracks) {
                this.rows.push(track as MediaTrack);
            }
        });
    }

    onChange(row: MediaTrack) {
        row._changed = this.changed = true;        
    }
    
    createRow(suffix: string, description: string, display_order: number) {
        this.labbcatService.labbcat.createMediaTrack(
            suffix, description, display_order,
            (row, errors, messages) => {
                if (errors) for (let message of errors) this.messageService.error(message);
                if (messages) for (let message of messages) this.messageService.info(message);
                // update the model with the field returned
                if (row) this.rows.push(row as MediaTrack);
                this.updateChangedFlag();
            });
    }
    
    deleteRow(row: MediaTrack) {
        if (confirm(`Are you sure you want to delete track with suffix "${row.suffix}"`)) {
            this.labbcatService.labbcat.deleteMediaTrack(row.suffix, (model, errors, messages) => {
                if (errors) for (let message of errors) this.messageService.error(message);
                if (messages) for (let message of messages) this.messageService.info(message);
                if (!errors) {
                    // remove from the model/view
                    this.rows = this.rows.filter(r => { return r !== row;});
                    this.updateChangedFlag();
                }});
        }
    }

    updateChangedRows() {
        this.rows
            .filter(r => r._changed)
            .forEach(r => this.updateRow(r));
    }

    updateRow(row: MediaTrack) {
        this.labbcatService.labbcat.updateMediaTrack(
            row.suffix, row.description, row.display_order,
            (mediaTrack, errors, messages) => {
                if (errors) for (let message of errors) this.messageService.error(message);
                if (messages) for (let message of messages) this.messageService.info(message);
                // update the model with the field returned
                const updatedRow = mediaTrack as MediaTrack;
                const i = this.rows.findIndex(r => {
                    return r.suffix == updatedRow.suffix; })
                this.rows[i] = updatedRow;
                this.updateChangedFlag();
            });
    }
    
    updateChangedFlag() {
        this.changed = false;
        for (let row of this.rows) {
            if (row._changed) {
                this.changed = true;
                break; // only need to find one
            }
        } // next row
    }
}
