import { Component, OnInit } from '@angular/core';

import { Response } from '../response';
import { Layer } from '../layer.ts';
import { MessageService } from '../message.service';
import { LabbcatService } from '../labbcat.service';

@Component({
  selector: 'app-admin-transcript-types',
  templateUrl: './admin-transcript-types.component.html',
  styleUrls: ['./admin-transcript-types.component.css']
})
export class AdminTranscriptTypesComponent implements OnInit {
    layer: Layer;
    types: string[];
    changed = false;
    
    constructor(
        private labbcatService: LabbcatService,
        private messageService: MessageService
    ) { }
    
    ngOnInit(): void {
        this.readLayer();
    }

    readLayer(): void {
        this.labbcatService.labbcat.getLayer("transcript_type", (layer, errors, messages) => {
            if (errors) for (let message of errors) this.messageService.error(message);
            if (messages) for (let message of messages) this.messageService.info(message);
            this.setLayer(layer as Layer);
        });
    }

    setLayer(layer: Layer): void {
        this.layer = layer as Layer;
        this.types = [];
        for (let label in layer.validLabels) {
            // for tanscript_type, label and description are the same
            this.types.push(label);
        }
        this.changed = false;
    }

    createRow(newType: string): boolean {
        if (this.types.indexOf(newType) >= 0) {
            this.messageService.error("Already exists: " + newType); // TODO i18n
            return false;
        }
        this.types.push(newType);
        this.changed = true;
        return true;
    }
    
    deleteRow(toDelete: string): boolean {
        if (this.types.length <= 1) {
            this.messageService.error("Cannot delete last record"); // TODO i18n
            return false;
        }
        const t = this.types.indexOf(toDelete);
        if (t > -1) {
            this.types.splice(t, 1);
        }
        this.changed = true;        
    }
    
    onChange() {
        this.changed = true;        
    }
    
    updating = false;
    updateChangedRows() {
        this.updating = true;
        this.layer.validLabels = {};
        for (let label of this.types) {
            // for tanscript_type, label and description are the same
            this.layer.validLabels[label] = label;
        }
        this.labbcatService.labbcat.saveLayer(
            this.layer, (layer, errors, messages) => {
                this.updating = false;
                this.changed = false;        
                if (errors) for (let message of errors) this.messageService.error(message);
                if (messages) for (let message of messages) this.messageService.info(message);
                if (layer) {
                    if (!messages) {
                        this.messageService.info("Updated transcript types"); // TODO i18n
                    }
                    this.setLayer(layer);
                } else {
                    this.readLayer();
                }
            });
    }
}
