import { Component, OnInit } from '@angular/core';

import { Response } from '../response';
import { Corpus } from '../corpus';
import { MessageService } from '../message.service';
import { LabbcatService } from '../labbcat.service';

@Component({
  selector: 'app-admin-corpora',
  templateUrl: './admin-corpora.component.html',
  styleUrls: ['./admin-corpora.component.css']
})
export class AdminCorporaComponent implements OnInit {
    languages: any[];
    rows: Corpus[];
    changed = false;
    
    constructor(
        private labbcatService: LabbcatService,
        private messageService: MessageService
    ) { }
    
    ngOnInit(): void {
        this.readLanguages();
        this.readRows();
    }

    readRows(): void {
        this.labbcatService.labbcat.readCorpora((corpora, errors, messages) => {
            this.rows = [];
            for (let corpus of corpora) {
                this.rows.push(corpus as Corpus);
            }
        });
    }

    readLanguages(): void {
        this.labbcatService.labbcat.getLayer("transcript_language", (layer, errors, messages) => {
            this.languages = [];
            for (let label in layer.validLabels) {
                if (label) {
                    this.languages.push({
                        label: label,
                        description: layer.validLabels[label]});
                }
            }
        });
    }

    onChange(row: Corpus) {
        row._changed = this.changed = true;        
    }
    
    createRow(name: string, language: string, description: string) {
        this.labbcatService.labbcat.createCorpus(
            name, language, description,
            (row, errors, messages) => {
                if (errors) for (let message of errors) this.messageService.error(message);
                for (let message of messages) this.messageService.info(message);
                // update the model with the field returned
                if (row) this.rows.push(row as Corpus);
                this.updateChangedFlag();
            });
    }
    
    deleteRow(row: Corpus) {
        if (confirm(`Are you sure you want to delete ${row.corpus_name}`)) {
            this.labbcatService.labbcat.deleteCorpus(row.corpus_name, (model, errors, messages) => {
                if (errors) for (let message of errors) this.messageService.error(message);
                for (let message of messages) this.messageService.info(message);
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

    updateRow(row: Corpus) {
        this.labbcatService.labbcat.updateCorpus(
            row.corpus_name, row.corpus_language, row.corpus_description,
            (corpus, errors, messages) => {
                if (errors) for (let message of errors) this.messageService.error(message);
                for (let message of messages) this.messageService.info(message);
                // update the model with the field returned
                const updatedRow = corpus as Corpus;
                const i = this.rows.findIndex(r => {
                    return r.corpus_name == updatedRow.corpus_name; })
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
