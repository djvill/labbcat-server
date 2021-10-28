import Plugin from '@ckeditor/ckeditor5-core/src/plugin';

import { toWidget, toWidgetEditable } from '@ckeditor/ckeditor5-widget/src/utils';
import Widget from '@ckeditor/ckeditor5-widget/src/widget';
import InsertUtteranceCommand from './insertutterancecommand';

export default class UtteranceEditing extends Plugin {
    static get requires() {
        return [ Widget ];
    }
    init() {
        this._defineSchema();
        this._defineConverters();
        this.editor.commands.add( 'insertUtterance', new InsertUtteranceCommand( this.editor ) );
    }
    _defineSchema() {
        const schema = this.editor.model.schema;
        
        schema.register( 'utterance', {
            // Behaves like a self-contained object (e.g. an image).
            isObject: true,
            
            // Allow in places where other blocks are allowed (e.g. directly in the root).
            allowWhere: '$block'
        } );
        
        schema.register( 'utteranceAudio', {
            // Cannot be split or left by the caret.
            isLimit: true,
            
            allowIn: 'utterance',
            allowAttributes: ['source','controls'],
            
            // Allow content which is allowed in blocks (i.e. text with attributes).
            allowContentOf: '$block'
        } );
        
        schema.register( 'utteranceDescription', {
            // Cannot be split or left by the caret.
            isLimit: true,
            
            allowIn: 'utterance',

            // Allow content which is allowed in the root (e.g. paragraphs).
            allowContentOf: '$root'
        } );

        schema.addChildCheck( ( context, childDefinition ) => {
            if ( context.endsWith( 'utteranceDescription' ) && childDefinition.name == 'utterance' ) {
                return false;
            }
        } );
    }
    _defineConverters() {
        const conversion = this.editor.conversion;

        // <utterance> converters
        conversion.for( 'upcast' ).elementToElement( {
            model: 'utterance',
            view: {
                name: 'section',
                classes: 'utterance'
            }
        } );
        conversion.for( 'dataDowncast' ).elementToElement( {
            model: 'utterance',
            view: {
                name: 'section',
                classes: 'utterance'
            }
        } );
        conversion.for( 'editingDowncast' ).elementToElement( {
            model: 'utterance',
            view: ( modelElement, { writer: viewWriter } ) => {
                const section = viewWriter.createContainerElement( 'section', { class: 'utterance' } );
                
                return toWidget( section, viewWriter, { label: 'utterance widget' } );
            }
        } );
        
        // <utteranceAudio> converters
        conversion.for( 'upcast' ).elementToElement( {
            model: 'utteranceAudio',
            view: {
                name: 'audio',
                classes: 'utterance-audio'
            }
        } );
        conversion.for( 'dataDowncast' ).elementToElement( {
            model: 'utteranceAudio',
            view: {
                name: 'audio',
                classes: 'utterance-audio'
            }
        } );
        conversion.for( 'editingDowncast' ).elementToElement( {
            model: 'utteranceAudio',
            view: ( modelElement, { writer: viewWriter } ) => {
                // Note: You use a more specialized createEditableElement() method here.
                const audio = viewWriter.createEditableElement( 'audio', { class: 'utterance-audio' } );
                
                return toWidgetEditable( audio, viewWriter );
            }
        } );
        conversion.attributeToAttribute( { model: 'source', view: 'src' } );
        conversion.attributeToAttribute( { model: 'controls', view: 'controls' } );
        
        // <utteranceDescription> converters
        conversion.for( 'upcast' ).elementToElement( {
            model: 'utteranceDescription',
            view: {
                name: 'div',
                classes: 'utterance-description'
            }
        } );
        conversion.for( 'dataDowncast' ).elementToElement( {
            model: 'utteranceDescription',
            view: {
                name: 'div',
                classes: 'utterance-description'
            }
        } );
        conversion.for( 'editingDowncast' ).elementToElement( {
            model: 'utteranceDescription',
            view: ( modelElement, { writer: viewWriter } ) => {
                // Note: You use a more specialized createEditableElement() method here.
                const div = viewWriter.createEditableElement( 'div', { class: 'utterance-description' } );
                
                return toWidgetEditable( div, viewWriter );
            }
        } );
    }
}
