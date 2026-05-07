export interface UploadedFile {
  fieldname: string;
  originalName: string;
  encoding: string;
  mimetype: string;
  size: number;
  path: string;
  url?: string;
}
