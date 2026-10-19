package net.vulkanmod.render.sky;

public final class CloudGridCheck {
    public static void main(String[] args) {
        var solid = new CloudRenderer.CloudGrid(new int[] {-1, -1, -1, -1}, 2);
        for (byte faces : solid.renderFaces) if (faces != 3) throw new IllegalStateException("Internal cloud faces retained");
        var isolated = new CloudRenderer.CloudGrid(new int[] {-1, 0, 0, 0}, 2);
        if (isolated.renderFaces[0] != 63 || isolated.renderFaces[1] != 0)
            throw new IllegalStateException("Cloud boundary/alpha classification");
        if (isolated.getWrappedIdx(-1, -1) != 3 || isolated.getWrappedIdx(2, 2) != 0)
            throw new IllegalStateException("Negative cloud texture wrapping");
        System.out.println("Cloud grid PASS: internal-face removal, boundary faces, alpha, negative wrapping");
    }
}
