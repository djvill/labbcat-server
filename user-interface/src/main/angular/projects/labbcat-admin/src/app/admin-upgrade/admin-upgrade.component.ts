import { Component, OnInit } from '@angular/core';
import { environment } from '../../environments/environment';

import { MessageService, LabbcatService, SerializationDescriptor } from 'labbcat-common';
import { AdminComponent } from '../admin-component';

@Component({
  selector: 'app-admin-upgrade',
  templateUrl: './admin-upgrade.component.html',
  styleUrl: './admin-upgrade.component.css'
})
export class AdminUpgradeComponent extends AdminComponent implements OnInit {
    
    baseUrl: string;
    imagesLocation = environment.imagesLocation;
    war: File;
    uploading = false;
    percentComplete: number;
    id: string;
    newVersion: string;
    oldVersion: string;
    migration: boolean;
    downgrade: boolean;
    cancelling = false;
    confirming = false;
    monitoring = false;
    upgradeMessages: string[];
    lastException: string;
    stackTrace: string;
    upgradeComplete = false;

    constructor(
        labbcatService: LabbcatService,
        messageService: MessageService,
    ) {
        super(labbcatService, messageService);
    }
    
    ngOnInit(): void {
        this.labbcatService.labbcat.getId((url, errors, messages) => {
            if (errors) errors.forEach(m => this.messageService.error(m));
            if (messages) messages.forEach(m => this.messageService.info(m));
            this.baseUrl = url;
        });
    }
    /** Called when an upgrader file is selected */
    selectFile(files: File[]): void {
        if (files.length == 0) {
            this.war = null;
            return;
        }
        this.war = files[0]
        if (!this.war.name.endsWith(".war")
            && !this.war.name.endsWith(".zip")) {
            this.messageService.error("File must be a LaBB-CAT installer file, e.g. labbcat.war"); // TODO i18n
            this.war = null;
            return;
        }
        this.upload();
    }
    
    upload() {
        this.uploading = true;

        // post file
        const fd = new FormData();
        fd.append("war", this.war);
        const upload = this.labbcatService.labbcat.createRequest(
            "upgrade", null, (model, errors, messages) => {
                this.uploading = false;
                if (errors) errors.forEach(m => this.messageService.error(m));
                if (messages) messages.forEach(m => this.messageService.info(m));
                if (model && model.id) {
                    this.id = model.id;
                    this.oldVersion = model.oldVersion;
                    this.newVersion = model.newVersion;
                    this.migration = model.migration;
                    this.downgrade = model.newVersion < model.oldVersion;
                }
            },
            this.baseUrl+"api/admin/upgrade", "POST");
        upload.upload.addEventListener("progress", (e)=> { // progress
            if (e.lengthComputable) {
                // upload goes up to 50% only
	        this.percentComplete = Math.round(e.loaded * 100 / e.total);
            }
        }, false);
        
        try {
            upload.send(fd);
        } catch (x) {
            this.messageService.error(x);
        }
    }
    
    confirm() {
        this.confirming = true;
        const confirm = this.labbcatService.labbcat.createRequest(
            "upgrade", null, (model, errors, messages) => {
                this.confirming = false;
                if (errors) errors.forEach(m => this.messageService.error(m));
                if (messages) messages.forEach(m => this.messageService.info(m));
                if (!errors) {
                    this.id = null;
                    this.monitorProgress();
                }
            },
            `${this.baseUrl}api/admin/upgrade/${this.id}`, "PUT");
        
        try {
            confirm.send();
        } catch (x) {
            this.messageService.error(x);
        }
    }
    
    cancel() {
        this.cancelling = true;
        const cancel = this.labbcatService.labbcat.createRequest(
            "upgrade", null, (model, errors, messages) => {
                this.cancelling = false;
                if (errors) errors.forEach(m => this.messageService.error(m));
                if (messages) messages.forEach(m => this.messageService.info(m));
                if (!errors) {
                    this.war = this.id = this.newVersion = this.oldVersion = null;
                }
            },
            `${this.baseUrl}api/admin/upgrade/${this.id}`, "DELETE");
        
        try {
            cancel.send();
        } catch (x) {
            this.messageService.error(x);
        }
    }
    
    monitorProgress() {
        this.monitoring = true;
        const monitor = this.labbcatService.labbcat.createRequest(
            "upgrade", null, (model, errors, messages) => {
                if (errors) errors.forEach(m => this.messageService.error(m));
                if (messages) messages.forEach(m => this.messageService.info(m));
                if (model) {
                    this.percentComplete = model.percentComplete;
                    if (model.messages) {
                        this.upgradeMessages = model.messages;
                        window.setTimeout(()=>{
                            try { // scroll to the last message
                                document.getElementById(
                                    'message-'+(this.upgradeMessages.length-1))
                                    .scrollIntoView({
                                        behavior: "smooth",
                                        block: "center"
                                    });
                            } catch (x) {}
                        }, 100);
                    }
                    if (model.lastException) {
                        this.lastException = model.lastException;
                        this.stackTrace = model.stackTrace;
                        return; // stop monitoring
                    }
                    if (!model.running
                        && !this.lastException && model.version >= this.newVersion) {
                        this.id = this.newVersion = this.oldVersion = null;
                        this.monitoring = false;
                        this.upgradeComplete = true;
                        return; // stop monitoring
                    }
                } 
                window.setTimeout(()=>{
                    this.monitorProgress();
                }, 500);
            },
            `${this.baseUrl}api/admin/upgrade`, "GET");
        
        try {
            monitor.send();
        } catch (x) {
            this.messageService.error(x);
        }
    }
}
