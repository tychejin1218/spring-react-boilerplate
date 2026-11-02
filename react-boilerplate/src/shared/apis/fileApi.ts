import axios from 'axios';
import api from '@/shared/apis/instance';
import type { ApiResponse } from '@/shared/types/api';

export interface PresignedUrlResponse {
  presignedUrl: string;
  objectUrl: string;
}

export const getPresignedUrl = (fileName: string, contentType: string): Promise<ApiResponse<PresignedUrlResponse>> =>
  api
    .get<ApiResponse<PresignedUrlResponse>>('/files/presigned-url', {
      params: { fileName, contentType },
    })
    .then((res) => res.data);

export const uploadToPresignedUrl = (url: string, file: File): Promise<void> =>
  axios.put(url, file, { headers: { 'Content-Type': file.type } }).then(() => undefined);
