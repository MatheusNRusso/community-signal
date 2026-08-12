import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'replace', standalone: true })
export class ReplacePipe implements PipeTransform {
  transform(value: string, find: string, replaceWith: string): string {
    return value ? value.split(find).join(replaceWith) : value;
  }
}
