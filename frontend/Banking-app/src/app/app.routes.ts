import { Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { AboutComponent } from './about/about.component';
import { ContactComponent } from './contact/contact.component';  
import { NotFoundComponent } from './not-found/not-found.component';
import { ResilienceComponent } from './resilience/resilience.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'about', component: AboutComponent },
  { path: 'contact', component: ContactComponent },
  { path: 'resilience', component: ResilienceComponent },
  { path: '**', component: NotFoundComponent }, 
];

