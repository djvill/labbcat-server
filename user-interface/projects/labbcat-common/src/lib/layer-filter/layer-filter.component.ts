import { Component, OnInit, Input, EventEmitter, Output } from '@angular/core';

import { Layer } from '../layer';

@Component({
  selector: 'lib-layer-filter',
  templateUrl: './layer-filter.component.html',
  styleUrls: ['./layer-filter.component.css']
})
export class LayerFilterComponent implements OnInit {

    @Input() layer: Layer;
    @Input() values: string[];
    @Output() changeValues = new EventEmitter<string[]>();
    inputType = "regexp";
    otherAllowed = false;
    
    constructor() { }
    
    ngOnInit(): void {
        console.log("this.values = " + JSON.stringify(this.values));
        this.determineInputType();
    }

    determineInputType(): void {
        if (this.layer.validLabels && Object.keys(this.layer.validLabels).length > 0) {
            this.inputType = "select";
            this.otherAllowed = /.*other.*/.test(this.layer.style);
        } else if (this.layer.type == "number") {
            this.inputType = "range";
            if (!this.values || !this.values.length) {
                this.values = ["",""];
            } else if (this.values.length == 1) {
                this.values = [this.values[0],""];
            }
        } else {
            this.inputType = "regexp";
        }
    }
    
    handleTextChange(value: string): void {
        this.values = [value]
        this.changeValues.emit(this.values);
    }
    
    handleRangeChange(i: number, value: string): void {
        this.values[i] = value;
        this.changeValues.emit(this.values);
    }

    handleCheckboxClick(label: string, checked: boolean): void {
        let newValues = [].concat(this.values);
        if (checked) {
            newValues.push(label);
        } else {
            newValues = newValues.filter(l => l != label);
        }
        this.changeValues.emit(newValues);
        this.values = newValues;
    }

    validLabelKeys(): string[] {
        return Object.keys(this.layer.validLabels)
            .filter(k=>k.length > 0);
    }

}
