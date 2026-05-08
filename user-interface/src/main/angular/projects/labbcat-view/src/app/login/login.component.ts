import { Inject, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { MessageService, LabbcatService } from 'labbcat-common';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

    username: string;
    password: string;
    error = false;
    
    // optionally replace "User ID or pass phrase incorrect" errors when operating in a maintenance mode, where admins temporarily turn off user accounts to conserve server resources (e.g., while performing a demo)
    // NOTE: This doesn't actually trigger maintenance mode. To do that, modify the MySQL miner_user table
    demoMode = false;
    demoMessage = "APLS is currently operating in 'demo mode'. We've temporarily disabled user logins to conserve server resources while we demonstrate APLS. We'll turn off 'demo mode' in less than an hour."

    constructor(
        private labbcatService: LabbcatService,
        private messageService: MessageService,
        @Inject('environment') private environment,
        private route: ActivatedRoute,
        private router: Router
    ) { }
    
    ngOnInit(): void {
    }

    login(): void {
        this.labbcatService.login(this.username, this.password)
            .then((url)=>{
                // url is the previous URL requested from the server
                // this might be "j_security_check" or an api call or a resource file
                // requested by another browser tab, etc.
                // we don't want to redirect the user to these
                if (!url // no URL?
                    || url.endsWith("j_security_check") // login request
                    || url.endsWith("keepalive") // heartbeat
                    || /.*\/api\/.*/.test(url) // e.g. ...api/systemattributes/title
                    || url.endsWith("js/shiftclickcheckbox.js") // other resources
                    || /.*\.[a-z]+(\?.*)?$/.test(url)) { // e.g. ...style.css?20250224
                    url = this.environment.baseUrl;
                }
                window.location.assign(url + (window.location.hash || ""));
            })
            .catch((errors)=>{
                if (this.demoMode && errors.length == 1 && errors[0] == "User ID or pass phrase incorrect") {
                    this.messageService.info(this.demoMessage);
                } else {
                    errors.forEach(m => this.messageService.error(m));
                    this.error = true;
                }
            });
    }
}
