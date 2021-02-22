import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { RolePermission } from '../role-permission';
import { MessageService, LabbcatService, Response, Layer } from 'labbcat-common';
import { AdminComponent } from '../admin-component';

@Component({
  selector: 'app-admin-role-permissions',
  templateUrl: './admin-role-permissions.component.html',
  styleUrls: ['./admin-role-permissions.component.css']
})
export class AdminRolePermissionsComponent extends AdminComponent implements OnInit {
    attributes: Layer[];
    validEntities = {
        tiav : "Everything",
        t : "Transcript",
        iav : "Media",
        i : "Image",
        a : "Audio",
        v : "Video"
    };
    get availableEntityValues(): string[] {
        // try to ensure that only entities that are not already covered are available
        const existingEntities = this.rows.map(p=>p.entity);
        return Object.keys(this.validEntities).filter(
            // filter out matching entities
            validEntity=>!existingEntities.find(
                existingEntity=>{
                    // if the existing entity includes this valid entity
                    return new RegExp(existingEntity).test(validEntity)
                    // or the this valid entity includes the existing entity
                        || new RegExp(validEntity).test(existingEntity)
                }));
    }
    rows: RolePermission[];
    get existingEntities(): string[] { return this.rows.map(permission=>permission.entity); }
    role_id: string;

    constructor(
        labbcatService: LabbcatService,
        messageService: MessageService,
        private route: ActivatedRoute
    ) {
        super(labbcatService, messageService);
    }
    
    ngOnInit(): void {
        this.readAttributes();
        this.readRows();
    }

    readAttributes(): void {
        this.labbcatService.labbcat.getLayers((layers, errors, messages) => {
            this.attributes = [];
            for (let layer of layers) {
                if (layer.id == "corpus" // corpus
                    || (layer.parentId == "graph" // or transcript tag
                        && layer.alignment == 0
                        && layer.saturated
                        && layer.id != "transcript_type" // (not transcript_type TODO fix this)
                        && layer.id != "episode" // (not episode)
                        && layer.id != "participant")) { // (but not participant layer)
                    this.attributes.push(layer as Layer);                        
                }
            } // next layer
        });
    }

    readRows(): void {
        this.role_id = this.route.snapshot.paramMap.get('role_id');
        this.labbcatService.labbcat.readRolePermissions(
            this.role_id, (permissions, errors, messages) => {
                this.rows = [];
                for (let permission of permissions) {
                    this.rows.push(permission as RolePermission);
                }
            });
    }
    
    onChange(row: RolePermission) {
        row._changed = this.changed = true;        
    }

    creating = false;
    createRow(entity: string, layer: string, value_pattern: string) : boolean {
        this.creating = true;
        this.labbcatService.labbcat.createRolePermission(
            this.role_id, entity, layer, value_pattern,
            (row, errors, messages) => {
                this.creating = false;
                if (errors) errors.forEach(m => this.messageService.error(m));
                if (messages) messages.forEach(m => this.messageService.info(m));
                // update the model with the field returned
                if (row) this.rows.push(row as RolePermission);
                this.updateChangedFlag();
            });
        return true;
    }
    
    deleteRow(row: RolePermission) {
        row._deleting = true;
        if (confirm(`Are you sure you want to delete ${this.validEntities[row.entity]}`)) {
            this.labbcatService.labbcat.deleteRolePermission(
                this.role_id, row.entity, (model, errors, messages) => {
                    row._deleting = false;
                    if (errors) errors.forEach(m => this.messageService.error(m));
                    if (messages) messages.forEach(m => this.messageService.info(m));
                    if (!errors) {
                        // remove from the model/view
                        this.rows = this.rows.filter(r => { return r !== row;});
                        this.updateChangedFlag();
                    }});
        } else {
            row._deleting = false;
        }
    }

    updating = 0;
    updateChangedRows() {
        this.rows
            .filter(r => r._changed)
            .forEach(r => this.updateRow(r));
    }
    
    updateRow(row: RolePermission) {
        this.updating++;
        this.labbcatService.labbcat.updateRolePermission(
            this.role_id, row.entity, row.layer, row.value_pattern,
            (permission, errors, messages) => {
                this.updating--;
                if (errors) errors.forEach(m => this.messageService.error(m));
                if (messages) messages.forEach(m => this.messageService.info(m));
                // update the model with the field returned
                const updatedRow = permission as RolePermission;
                const i = this.rows.findIndex(r => {
                    return r.entity == updatedRow.entity; })
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
