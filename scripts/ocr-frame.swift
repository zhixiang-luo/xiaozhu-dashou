import AppKit
import Foundation
import Vision

guard CommandLine.arguments.count >= 2 else {
    fputs("usage: swift scripts/ocr-frame.swift IMAGE...\n", stderr)
    exit(2)
}

for path in CommandLine.arguments.dropFirst() {
    guard
        let image = NSImage(contentsOfFile: path),
        let cgImage = image.cgImage(forProposedRect: nil, context: nil, hints: nil)
    else {
        fputs("cannot read image: \(path)\n", stderr)
        continue
    }

    let request = VNRecognizeTextRequest()
    request.recognitionLevel = .accurate
    request.usesLanguageCorrection = true
    request.recognitionLanguages = ["zh-Hans", "en-US"]

    do {
        try VNImageRequestHandler(cgImage: cgImage).perform([request])
    } catch {
        fputs("OCR failed for \(path): \(error)\n", stderr)
        continue
    }

    print("=== \(path) ===")
    for observation in (request.results ?? []).sorted(by: {
        abs($0.boundingBox.midY - $1.boundingBox.midY) > 0.015
            ? $0.boundingBox.midY > $1.boundingBox.midY
            : $0.boundingBox.minX < $1.boundingBox.minX
    }) {
        guard let candidate = observation.topCandidates(1).first else {
            continue
        }
        let box = observation.boundingBox
        print(
            String(
                format: "%.4f\t%.4f\t%.4f\t%.4f\t%@",
                box.minX,
                1.0 - box.maxY,
                box.width,
                box.height,
                candidate.string
            )
        )
    }
}
