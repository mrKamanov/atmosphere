package android.print;

import android.content.Context;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.webkit.WebView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Запись PDF из WebView через Print API.
 * Класс в пакете android.print — иначе недоступны callback-конструкторы PrintDocumentAdapter.
 */
public final class DeputyHtmlPdfWriter {

    public interface ResultCallback {
        void onSuccess(byte[] pdfBytes);
        void onFailure();
    }

    private DeputyHtmlPdfWriter() {}

    public static void write(Context context, WebView webView, ResultCallback callback) {
        PrintAttributes attributes = new PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build();
        PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter("AtmosphereReport");
        File tempFile = new File(context.getCacheDir(), "deputy_report_" + System.currentTimeMillis() + ".pdf");

        adapter.onLayout(
            null,
            attributes,
            null,
            new PrintDocumentAdapter.LayoutResultCallback() {
                @Override
                public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                    try {
                        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                            tempFile,
                            ParcelFileDescriptor.MODE_CREATE
                                | ParcelFileDescriptor.MODE_READ_WRITE
                                | ParcelFileDescriptor.MODE_TRUNCATE
                        );
                        adapter.onWrite(
                            new PageRange[] { PageRange.ALL_PAGES },
                            pfd,
                            new CancellationSignal(),
                            new PrintDocumentAdapter.WriteResultCallback() {
                                @Override
                                public void onWriteFinished(PageRange[] pages) {
                                    try {
                                        pfd.close();
                                    } catch (IOException ignored) {
                                    }
                                    byte[] bytes = readAndDelete(tempFile);
                                    if (bytes != null) {
                                        callback.onSuccess(bytes);
                                    } else {
                                        callback.onFailure();
                                    }
                                }

                                @Override
                                public void onWriteFailed(CharSequence error) {
                                    try {
                                        pfd.close();
                                    } catch (IOException ignored) {
                                    }
                                    tempFile.delete();
                                    callback.onFailure();
                                }
                            }
                        );
                    } catch (IOException e) {
                        tempFile.delete();
                        callback.onFailure();
                    }
                }

                @Override
                public void onLayoutFailed(CharSequence error) {
                    tempFile.delete();
                    callback.onFailure();
                }
            },
            null
        );
    }

    private static byte[] readAndDelete(File file) {
        if (!file.exists()) {
            return null;
        }
        try (FileInputStream input = new FileInputStream(file)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } catch (IOException e) {
            return null;
        } finally {
            file.delete();
        }
    }
}
