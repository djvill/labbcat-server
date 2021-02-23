import { HostListener } from '@angular/core';

import { MessageService, LabbcatService, ComponentCanDeactivate } from 'labbcat-common';

// Base class for components that implement any kind of CRUD administration operations
export class EditComponent implements ComponentCanDeactivate {
    changed = false;
    
    constructor(
        protected labbcatService: LabbcatService,
        protected messageService: MessageService
    ) { }

    // guard against browser refresh, close, etc.
    @HostListener('window:beforeunload')
    canDeactivate(): boolean {
        return !this.changed;
    }
}
