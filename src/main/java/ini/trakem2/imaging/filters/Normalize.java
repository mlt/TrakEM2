/*-
 * #%L
 * TrakEM2 plugin for ImageJ.
 * %%
 * Copyright (C) 2005 - 2022 Albert Cardona, Stephan Saalfeld and others.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package ini.trakem2.imaging.filters;

import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

import java.util.Map;

public class Normalize implements IFilter
{
	protected double targetMean = 0;
	protected double targetStdDev = 0;
	
	public Normalize() {}

	public Normalize(double targetMean, double targetStdDev) {
		this.targetMean = targetMean;
		this.targetStdDev = targetStdDev;
	}
	
	public Normalize(Map<String,String> params) {
		try {
			this.targetMean = Double.parseDouble(params.get("mean"));
			this.targetStdDev = Integer.parseInt(params.get("stddev"));
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Cannot create RankFilter!", nfe);
		}
	}
	@Override
	public ImageProcessor process(ImageProcessor ip) {
		if (ip instanceof ColorProcessor) {
			FloatProcessor r = normalize(ip.toFloat(0, null)),
			               g = normalize(ip.toFloat(1, null)),
			               b = normalize(ip.toFloat(2, null));
			int[] p = new int[ip.getWidth() * ip.getHeight()];
			ColorProcessor cp = new ColorProcessor(ip.getWidth(), ip.getHeight(), p);
			final float[] rp = (float[]) r.getPixels(),
			              gp = (float[]) g.getPixels(),
			              bp = (float[]) b.getPixels();
			for (int i=0; i<p.length; ++i) {
				p[i] = ((int)rp[i] << 16) | ((int)gp[i] << 8) | (int)bp[i];
			}
			return cp;
		}
		final FloatProcessor fp = normalize((FloatProcessor)ip.convertToFloat());
		if (ip instanceof FloatProcessor) {
			return fp;
		}
		final int len = ip.getWidth() * ip.getHeight();
		for (int i=0; i<len; ++i) {
			ip.setf(i, fp.get(i));
		}
		return ip;
	}

	private FloatProcessor normalize(FloatProcessor fp) {
		double s = 0;
		final int len = fp.getWidth() * fp.getHeight();
		final float[] p = (float[]) fp.getPixels();
		// Compute mean
		for (int i=0; i<len; ++i) s += p[i];
		final double mean = s / len;
		// Compute stdDev
		s = 0;
		for (int i=0; i<len; ++i) s += Math.pow(p[i] - mean, 2);
		//final double stdDev = Math.sqrt(s / (len - 1));
		//final double K = targetStdDev / stdDev;
		final double K = targetStdDev / Math.sqrt(s / (len - 1)); // save one register
		for (int i=0; i<len; ++i) {
			//p[i] = (float)((((p[i] - mean) / stdDev) * targetStdDev) + targetMean);
			p[i] = (float)(((p[i] - mean) * K) + targetMean); // save one division
		}
		return fp;
	}

	@Override
	public String toXML(String indent) {
		return new StringBuilder(indent)
		.append("<t2_filter class=\"").append(getClass().getName())
		.append("\" mean=\"").append(targetMean)
		.append("\" stddev=\"").append(targetStdDev)
		.append("\" />\n").toString();
	}
	
	
	@Override
	public boolean equals(final Object o) {
		if (null == o) return false;
		if (o.getClass() == getClass()) {
			final Normalize nr = (Normalize)o;
			return targetMean == nr.targetMean && targetStdDev == nr.targetStdDev;
		}
		return false;
	}
}
