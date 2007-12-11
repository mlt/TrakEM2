/**

TrakEM2 plugin for ImageJ(C).
Copyright (C) 2007 Albert Cardona.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA. 

You may contact Albert Cardona at acardona at ini.phys.ethz.ch
Institute of Neuroinformatics, University of Zurich / ETH, Switzerland.
**/

package ini.trakem2.vector;

import ini.trakem2.utils.IJError;
import ini.trakem2.utils.Utils;
import ini.trakem2.utils.Vector3D;

import ij.measure.Calibration;

import java.util.Arrays;
import java.util.Random;
import Jama.Matrix;

public class VectorString3D implements VectorString {

	/** Points. */
	private double[] x, y, z;
	/** Vectors, after resampling. */
	private double[] vx, vy, vz;
	/** Relative vectors, after calling 'relative()'. */
	private double[] rvx, rvy, rvz;
	/** Length of points and vectors - since arrays may be a little longer. */
	private int length = 0;
	/** The point interdistance after resampling. */
	private double delta = 0;

	private boolean closed = false;

	private Calibration cal = null;

	/** Dependent arrays that will get resampled along. */
	private double[][] dep;

	// DANGER: the orientation of the curve can't be checked like in 2D. There is no up and down in the 3D space.

	public VectorString3D(double[] x, double[] y, double[] z, boolean closed) throws Exception {
		if (!(x.length == y.length && x.length == z.length)) throw new Exception("x,y,z must have the same length.");
		this.length = x.length;
		this.x = x;
		this.y = y;
		this.z = z;
		this.closed = closed;
	}

	/** Add an array that will get resampled along; must be of the same length as the value returned by length() */
	public void addDependent(final double[] a) throws Exception {
		if (a.length != this.length) throw new Exception("Dependent array must be of the same size as thevalue returned by length()");
		if (null == dep) {
			dep = new double[1][];
			dep[0] = a;
		} else {
			// resize and append
			double[][] dep2 = new double[dep.length + 1][];
			for (int i=0; i<dep.length; i++) dep2[i] = dep[i];
			dep2[dep.length] = a;
			dep = dep2;
		}
	}
	public double[] getDependent(final int i) {
		return dep[i];
	}

	/** Return the average point interdistance. */
	public double getAverageDelta() {
		double d = 0;
		for (int i=length -1; i>0; i--) {
			d += Math.sqrt( Math.pow(x[i] - x[i-1], 2) + Math.pow(y[i] - y[i-1], 2) + Math.pow(z[i] - z[i-1], 2));
		}
		return d / length;
	}

	/** Homogenize the average point interdistance to 'delta'. */
	public void resample(double delta) {
		if (Math.abs(delta - this.delta) < 0.0000001) {
			// delta is the same
			return;
		}
		this.delta = delta; // store for checking purposes
		this.resample();
	}

	public int length() { return length; }
	public double[] getPoints(final int dim) {
		switch (dim) {
			case 0: return x;
			case 1: return y;
			case 2: return z;
		}
		return null;
	}
	public double[] getVectors(final int dim) {
		switch (dim) {
			case 0: return vx;
			case 1: return vy;
			case 2: return vz;
		}
		return null;
	}
	public double getPoint(final int dim, final int i) {
		switch (dim) {
			case 0: return x[i];
			case 1: return y[i];
			case 2: return z[i];
		}
		return 0;
	}
	public double getVector(final int dim, final int i) {
		switch (dim) {
			case 0: return vx[i];
			case 1: return vy[i];
			case 2: return vy[i];
		}
		return 0;
	}
	public double getRelativeVector(final int dim, final int i) {
		switch (dim) {
			case 0: return rvx[i];
			case 1: return rvy[i];
			case 2: return rvy[i];
		}
		return 0;
	}
	public boolean isClosed() { return closed; }

	public void debug() {
		Utils.log2("#### " + getClass().getName() + " ####");
		for (int i=0; i<x.length; i++) {
			Utils.log2( i + ": " + x[i] + "," + y[i] + ", " + z[i]);
		}
		Utils.log2("#### END ####");
	}


	private class ResamplingData {

		private double[] rx, ry, rz,
				 vx, vy, vz;
		private double[][] dep;

		/** Initialize with a starting length. */
		ResamplingData(final int length, final double[][] dep) {
			// resampled points
			rx = new double[length];
			ry = new double[length];
			rz = new double[length];
			// vectors
			vx = new double[length];
			vy = new double[length];
			vz = new double[length];
			// dependents
			if (null != dep) this.dep = new double[dep.length][length];
		}
		/** Arrays are enlarged if necessary.*/
		final void setP(final int i, final double xval, final double yval, final double zval) {
			if (i >= rx.length) resize(i+10);
			this.rx[i] = xval;
			this.ry[i] = yval;
			this.rz[i] = zval;
		}
		/** Arrays are enlarged if necessary.*/
		final void setV(final int i, final double xval, final double yval, final double zval) {
			if (i >= rx.length) resize(i+10);
			this.vx[i] = xval;
			this.vy[i] = yval;
			this.vz[i] = zval;
		}
		/** Arrays are enlarged if necessary.*/
		final void setPV(final int i, final double rxval, final double ryval, final double rzval, final double xval, final double yval, final double zval) {
			if (i >= rx.length) resize(i+10);
			this.rx[i] = rxval;
			this.ry[i] = ryval;
			this.rz[i] = rzval;
			this.vx[i] = xval;
			this.vy[i] = yval;
			this.vz[i] = zval;
		}
		final void resize(final int new_length) {
			this.rx = Utils.copy(this.rx, new_length);
			this.ry = Utils.copy(this.ry, new_length);
			this.rz = Utils.copy(this.rz, new_length);
			this.vx = Utils.copy(this.vx, new_length);
			this.vy = Utils.copy(this.vy, new_length);
			this.vz = Utils.copy(this.vz, new_length);
			if (null != dep) {
				// java doesn't have generators! ARGH
				double[][] dep2 = new double[dep.length][];
				for (int i=0; i<dep.length; i++) dep2[i] = Utils.copy(dep[i], new_length);
				dep = dep2;
			}
		}
		final double x(final int i) { return rx[i]; }
		final double y(final int i) { return ry[i]; }
		final double z(final int i) { return rz[i]; }
		/** Distance from point rx[i],ry[i], rz[i] to point x[j],y[j],z[j] */
		final double distance(final int i, final double x, final double y, final double z) {
			return Math.sqrt(Math.pow(x - rx[i], 2)
				       + Math.pow(y - ry[i], 2)
				       + Math.pow(z - rz[i], 2));
		}
		final void put(final VectorString3D vs, final int length) {
			vs.x = Utils.copy(this.rx, length); // crop away empty slots
			vs.y = Utils.copy(this.ry, length);
			vs.z = Utils.copy(this.rz, length);
			vs.vx = Utils.copy(this.vx, length);
			vs.vy = Utils.copy(this.vy, length);
			vs.vz = Utils.copy(this.vz, length);
			vs.length = length;
			if (null != dep) {
				vs.dep = new double[dep.length][];
				for (int i=0; i<dep.length; i++) vs.dep[i] = Utils.copy(dep[i], length);
			}
		}
		final void setDeps(final int i, final double[][] src_dep, final int[] ahead, final double[] weight, final int len) {
			if (null == dep) return;
			if (i >= rx.length) resize(i+10);
			//
			for (int k=0; k<dep.length; k++) {
				for (int j=0; j<len; j++) {
					dep[k][i] += src_dep[k][ahead[j]] * weight[j];
				}
			} // above, the ahead and weight arrays (which have the same length) could be of larger length than the given 'len', thus len is used.
		}
	}

	static private class Vector {
		private double x, y, z;
		private double length;
		Vector(final double x, final double y, final double z) {
			set(x, y, z);
		}
		final void set(final double x, final double y, final double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.length = computeLength();
		}
		final void normalize() {
			if (0 == length) return;
			this.x /= length;
			this.y /= length;
			this.z /= length;
			this.length = computeLength(); // should be 1
		}
		final double computeLength() {
			return Math.sqrt(x*x + y*y + z*z);
		}
		final double length() {
			return length;
		}
		final void scale(final double factor) {
			this.x *= factor;
			this.y *= factor;
			this.z *= factor;
			this.length = computeLength();
		}
		final void add(final Vector v, final boolean compute_length) {
			this.x += v.x;
			this.y += v.y;
			this.z += v.z;
			if (compute_length) this.length = computeLength();
		}
		final void setLength(final double len) {
			normalize();
			scale(len);
		}
		final void put(final int i, final ResamplingData r) {
			r.setPV(i, r.x(i-1) + this.x, r.y(i-1) + this.y, r.z(i-1) + this.z, this.x, this.y, this.z);
		}
		/** As row. */
		final void put(final double[] d) {
			d[0] = x;
			d[1] = y;
			d[2] = z;
		}
		/** As column. */
		final void put(final double[][] d, final int col) {
			d[0][col] = x;
			d[1][col] = y;
			d[2][col] = z;
		}
		final void put(final int i, final double[] x, final double[] y, final double[] z) {
			x[i] = this.x;
			y[i] = this.y;
			z[i] = this.z;
		}
		final Vector getCrossProduct(final Vector v) {
			// (a1; a2; a3) x (b1; b2; b3) = (a2b3 - a3b2; a3b1 - a1b3; a1b2 - a2b1)
			return new Vector(y * v.z - z * v.y,
					  z * v.x - x * v.z,
					  x * v.y - y * v.x);
		}
	}

	private final void recalculate(final double[] w, final int length, final double sum_) {
		double sum = 0;
		int q;
		for (q=0; q<length; q++) {
			w[q] = w[q] / sum_;
			sum += w[q];
		}
		double error = 1.0 - sum;
		// make it be an absolute value
		if (error < 0.0) {
			error = -error;
		}
		if (error < 0.005) {
			w[0] += 1.0 - sum;
		} else if (sum > 1.0) {
			recalculate(w, length, sum);
		}
	}

	private void resample() {
		// parameters
		final int MAX_AHEAD = 6;
		final double MAX_DISTANCE = 2.5 * delta;

		// convenient data carrier and editor
		final ResamplingData r = new ResamplingData(this.length, this.dep);
		final Vector vector = new Vector(0, 0, 0);

		// first resampled point is the same as point zero
		r.setP(0, x[0], y[0], z[0]);
		// the first vector is 0,0,0 unless the path is closed, in which case it contains the vector from last-to-first.

		// index over x,y,z
		int i = 1;
		// index over rx,ry,rz (resampled points)
		int j = 1;
		// some vars
		int t, s, ii, u, iu, k;
		int prev_i = i;
		double dist_ahead, dist1, dist2, sum;
		final double[] w = new double[MAX_AHEAD];
		final double[] distances = new double[MAX_AHEAD];
		final Vector[] ve = new Vector[MAX_AHEAD];
		int next_ahead;
		for (next_ahead = 0; next_ahead < MAX_AHEAD; next_ahead++) ve[next_ahead] = new Vector(0, 0, 0);
		final int[] ahead = new int[MAX_AHEAD];

		try {

		// start infinite loop
		for (;prev_i <= i;) {
			if (prev_i > i || (!closed && i == this.length -1)) break;
			// get distances of MAX_POINTs ahead from the previous point
			next_ahead = 0;
			for (t=0; t<MAX_AHEAD; t++) {
				s = i + t;
				// fix 's' if it goes over the end
				if (s >= this.length) {
					if (closed) s -= this.length;
					else break;
				}
				dist_ahead = r.distance(j-1, x[s], y[s], z[s]);
				if (dist_ahead < MAX_DISTANCE) {
					ahead[next_ahead] = s;
					distances[next_ahead] = dist_ahead;
					next_ahead++;
				}
			}
			if (0 == next_ahead) {
				// No points (ahead of the i point) are found within MAX_DISTANCE
				// Just use the next point as target towards which create a vector of length delta
				vector.set(x[i] - r.x(j-1), y[i] - r.y(j-1), z[i] - r.z(j-1));
				dist1 = vector.length();
				vector.setLength(delta);
				vector.put(j, r);
				if (null != dep) r.setDeps(j, dep, new int[]{i}, new double[]{1.0}, 1);

				//Utils.log2("j: " + j + " (ZERO)  " + vector.computeLength() + "  " + vector.length());

				//correct for point overtaking the not-close-enough point ahead in terms of 'delta_p' as it is represented in MAX_DISTANCE, but overtaken by the 'delta' used for subsampling:
				if (dist1 <= delta) {
					//look for a point ahead that is over distance delta from the previous j, so that it will lay ahead of the current j
					for (u=i; u<this.length; u++) {
						dist2 = Math.sqrt(Math.pow(x[u] - r.x(j-1), 2)
								+ Math.pow(y[u] - r.y(j-1), 2)
								+ Math.pow(z[u] - r.z(j-1), 2));
						if (dist2 > delta) {
							prev_i = i;
							i = u;
							break;
						}
					}
				}
			} else {
				// Compose a point ahead out of the found ones.
				//
				// First, adjust weights for the points ahead
				w[0] = distances[0] / MAX_DISTANCE;
				double largest = w[0];
				for (u=1; u<next_ahead; u++) {
					w[u] = 1 - (distances[u] / MAX_DISTANCE);
					if (w[u] > largest) {
						largest = w[u];
					}
				}
				// normalize weights: divide by largest
				sum = 0;
				for (u=0; u<next_ahead; u++) {
					w[u] = w[u] / largest;
					sum += w[u];
				}
				// correct error. The closest point gets the extra
				if (sum < 1.0) {
					w[0] += 1.0 - sum;
				} else {
					recalculate(w, next_ahead, sum);
				}
				// Now, make a vector for each point with the corresponding weight
				vector.set(0, 0, 0);
				for (u=0; u<next_ahead; u++) {
					iu = i + u;
					if (iu >= this.length) iu -= this.length;
					ve[u].set(x[iu] - r.x(j-1), y[iu] - r.y(j-1), z[iu] - r.z(j-1));
					ve[u].setLength(w[u] * delta);
					vector.add(ve[u], u == next_ahead-1); // compute the length only on the last iteration
				}
				// correct potential errors
				if (Math.abs(vector.length() - delta) > 0.00000001) {
					vector.setLength(delta);
				}
				// set
				vector.put(j, r);
				if (null != dep) r.setDeps(j, dep, ahead, w, next_ahead);

				//Utils.log2("j: " + j + "  (" + next_ahead + ")   " + vector.computeLength() + "  " + vector.length());


				// find the first point that is right ahead of the newly added point
				// so: loop through points that lay within MAX_DISTANCE, and find the first one that is right past delta.
				ii = i;
				for (k=0; k<next_ahead; k++) {
					if (distances[k] > delta) {
						ii = ahead[k];
						break;
					}
				}
				// correct for the case of unseen point (because all MAX_POINTS ahead lay under delta):
				prev_i = i;
				if (i == ii) {
					i = ahead[next_ahead-1] +1; //the one after the last.
					if (i >= this.length) {
						if (closed) i = i - this.length; // this.length is the length of the x,y,z, the original points
						else i = this.length -1;
					}
				} else {
					i = ii;
				}
			}
			//advance index in the new points
			j += 1;
		} // end of for loop

		} catch (Exception e) {
			e.printStackTrace();
			Utils.log2("Some data: x,y,z .length = " + x.length + "," + y.length + "," + z.length
				 + "\nj=" + j + ", i=" + i + ", prev_i=" + prev_i
					);
		}


		dist_ahead = r.distance(j-1, x[this.length-1], y[this.length-1], z[this.length-1]);

		//Utils.log2("delta: " + delta + "\nlast point: " + x[x.length-1] + ", " + y[y.length-1] + ", " + z[z.length-1]);
		//Utils.log2("last resampled point: x,y,z " + r.x(j-1) + ", " + r.y(j-1) + ", " + r.z(j-1));
		//Utils.log2("distance: " + dist_ahead);

		// see whether the subsampling terminated too early, and fill with a line of points.
		final int last_i = closed ? 0 : this.length -1;
		if (dist_ahead > delta*1.2) {
			System.out.println("resampling terminated too early. Why?");
			while (dist_ahead > delta*1.2) {
				// make a vector from the last resampled point to the last point
				vector.set(x[last_i] - r.x(j-1), y[last_i] - r.y(j-1), z[last_i] - r.z(j-1));
				// resize it to length delta
				vector.setLength(delta);
				vector.put(j, r);
				j++;
				dist_ahead = r.distance(j-1, x[last_i], y[last_i], z[last_i]);
			}
		}
		// done!
		r.put(this, j); // j acts as length of resampled points and vectors
		// vector at zero is left as 0,0 which makes no sense. Should be the last point that has no vector, or has it only in the event that the list of points is declared as closed: a vector to the first point. Doesn't really matter though, as long as it's clear: as of right now, the first point has no vector unless the path is closed, in which case it contains the vector from the last-to-first.
	}

	/** Reorder the arrays so that the index zero becomes new_zero -the arrays are circular. */
	public void reorder(final int new_zero) {
		int i, j;
		// copying
		double[] tmp = new double[this.length];
		double[] src;
		// x
		src = x;
		for (i=0, j=new_zero; j<length; i++, j++) { tmp[i] = src[j]; }
		for (j=0; j<new_zero; i++, j++) { tmp[i] = src[j]; }
		x = tmp;
		tmp = src;
		// y
		src = y;
		for (i=0, j=new_zero; j<length; i++, j++) { tmp[i] = src[j]; }
		for (j=0; j<new_zero; i++, j++) { tmp[i] = src[j]; }
		y = tmp;
		tmp = src;
		// z
		src = z;
		for (i=0, j=new_zero; j<length; i++, j++) { tmp[i] = src[j]; }
		for (j=0; j<new_zero; i++, j++) { tmp[i] = src[j]; }
		z = tmp;
		tmp = src;
		// vx
		src = vx;
		for (i=0, j=new_zero; j<length; i++, j++) { tmp[i] = src[j]; }
		for (j=0; j<new_zero; i++, j++) { tmp[i] = src[j]; }
		vx = tmp;
		tmp = src;
		// vy
		src = vy;
		for (i=0, j=new_zero; j<length; i++, j++) { tmp[i] = src[j]; }
		for (j=0; j<new_zero; i++, j++) { tmp[i] = src[j]; }
		vy = tmp;
		tmp = src;
		// vz
		src = vz;
		for (i=0, j=new_zero; j<length; i++, j++) { tmp[i] = src[j]; }
		for (j=0; j<new_zero; i++, j++) { tmp[i] = src[j]; }
		vz = tmp;
		tmp = src;
	}

	/** Subtracts vs2 vector j to this vector i and returns its length, without changing any data. */
	public double getDiffVectorLength(final int i, final int j, final VectorString vsb) {
		final VectorString3D vs = (VectorString3D)vsb;
		if (null == rvx || null == rvy || null == rvz) {
			// use absolute vectors
			final double dx = vx[i] - vs.vx[j];
			final double dy = vy[i] - vs.vy[j];
			final double dz = vz[i] - vs.vz[j];

			if (i == j){
				double leni = Math.sqrt(vx[i]*vx[i]
						      + vy[i]*vy[i]
						      + vz[i]*vz[i]);
				double lenj = Math.sqrt(vs.vx[j]*vs.vx[j]
						      + vs.vy[j]*vs.vy[j]
						      + vs.vz[j]*vs.vz[j]);

				/*
				Utils.log2("i: " + i + " len: " + leni + "\t\tj: " + j + " len:" + lenj
					+ "\n\t" + vx[i] + "\t\t\t" +  vs.vx[j]
					+ "\n\t" + vy[i] + "\t\t\t" +  vs.vy[j]
					+ "\n\t" + vz[i] + "\t\t\t" +  vs.vz[j]);
				*/
			}

			return Math.sqrt(dx*dx + dy*dy + dz*dz);

		} else {
			// use relative vectors
			final double dx = rvx[i] - vs.rvx[j];
			final double dy = rvy[i] - vs.rvy[j];
			final double dz = rvz[i] - vs.rvz[j];
			final double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
			if (j == i) /*(j > i-2 && j < i+2)*/ {
				//Utils.log2("rel: i,j,dist "+ i + ", " + j + ", " + dist + " dx,dy,dz: " + dx + ", " + dy + ", " + dz);
				double leni = Math.sqrt(rvx[i]*rvx[i]
						      + rvy[i]*rvy[i]
						      + rvz[i]*rvz[i]);
				double lenj = Math.sqrt(vs.rvx[j]*vs.rvx[j]
						      + vs.rvy[j]*vs.rvy[j]
						      + vs.rvz[j]*vs.rvz[j]);

				/*
				Utils.log2("i: " + i + " len: " + leni + "\t\tj: " + j + " len:" + lenj
					+ "\n\t" + rvx[i] + "\t\t\t" +  vs.rvx[j]
					+ "\n\t" + rvy[i] + "\t\t\t" +  vs.rvy[j]
					+ "\n\t" + rvz[i] + "\t\t\t" +  vs.rvz[j]);
				*/
			}
			return dist;
		}
	}

	public Object clone() {
		try {
			final VectorString3D vs = new VectorString3D(Utils.copy(x, length), Utils.copy(y, length), Utils.copy(z, length), closed);
			vs.delta = delta;
			if (null != vx) vs.vx = Utils.copy(vx, length);
			if (null != vy) vs.vy = Utils.copy(vy, length);
			if (null != vz) vs.vz = Utils.copy(vz, length);
			if (null != rvx) vs.rvx = Utils.copy(rvx, length);
			if (null != rvy) vs.rvy = Utils.copy(rvy, length);
			if (null != rvz) vs.rvz = Utils.copy(rvz, length);
			return vs;
		} catch (Exception e) {
			new IJError(e);
		}
		return null;
	}

	/** Create the relative vectors, that is, the differences of one vector to the next. In this way, vectors represent changes in the path and are independent of the actual path orientation in space. */
	public void relative() {
		// create vectors if not there yet
		if (null == vx || null == vy || null == vz) {
			resample(getAverageDelta());
		}
		rvx = new double[length];
		rvy = new double[length];
		rvz = new double[length];


		// TODO 2: also, it should test both orientations and select the best

		// TODO: there is a mistake. Both vectors, i and i-1, have to be rotated so that vector i becomes aligned with whatever axis, as long as all vectors get aligned to the same. In this way then one truly stores differences only. An easy approach is to store the angle difference, since then it will be a scalar value independent of the orientation of the vectors. As long as the angle is represented with a vector, fine.
		// Another alternative is, if both vectors are rotated, then the first is always on the axis and the second has the "difference" information already as is.
		// So for a vector c1,c2,c3 of length L, I want it as L,0,0 (for example) and then the second vector, as rotated, is the one to use as relative.
		// So: v[i-1], vL, and v[i]; then v[i-1] + W = vL, so W = vL - v[i-1], and then the diff vector is v[i] + W

		//double wx, wy, wz; // the W
		//double vLx; // vLy = vLz = 0

		// the first vector:
		if (closed) { // last to first
			rvx[0] = vx[0] + delta - vx[length-1];
			rvy[0] = vy[0] - vy[length-1];
			rvz[0] = vz[0] - vz[length-1];
		} // else, as open curve the first vector remains 0,0,0
		// fill in the rest:
		for (int i=1; i<length; i++) {
			// So: Johannes Schindelin said: vector Y is the rotated, X is the original
			// Y = A + R * (X - A)    where R is a rotation matrix (A is the displacement vector, 0,0,0 in this case)
			// so the axis of rotation is the cross product of {L,0,0} and i-1
			// (a1; a2; a3) x (b1; b2; b3) = (a2b3 - a3b2; a3b1 - a1b3; a1b2 - a2b1)
			//Vector axis = new Vector(0 - 0, 0 - delta * vz[i-1], delta * vy[i-1] - 0);
			Vector vL = new Vector(delta, 0, 0);
			vL.normalize();		// this line can be reduced to new Vector(1,0,0);
			Vector vA = new Vector(vx[i-1], vy[i-1], vz[i-1]);
			vA.normalize();
			Vector vQ = vA.getCrossProduct(vL); // new Vector(0, - delta * vz[i-1], delta * vy[i-1]); // cross product of vL and vA, i.e. a vector perpendicular to it
			vQ.normalize();
			Vector vQQ = vQ.getCrossProduct(vL);
			vQQ.normalize();

			// the matrices expect the vectors as columns, not as rows! ARGH! Solved.

			// now, vQ,vL,vQQ define an orthogonal system
			// (vQ'; vL'; vQ' x vL')
			double[][] m1 = new double[3][3];
			vQ.put(m1, 0);
			vL.put(m1, 1);
			vQQ.put(m1, 2);
			Matrix mat1 = new Matrix(m1);

			// (vQ'; vA'; vQ' x vA')^T            // Johannes wrote vB to mean vA. For me vB is the second vector
			double[][] m2 = new double[3][3];
			vQ.put(m2, 0);
			vA.put(m2, 1);
			Vector vQ2 = vQ.getCrossProduct(vA);
			vQ2.normalize();
			vQ2.put(m2, 2);
			Matrix mat2 = new Matrix(m2).transpose();

			Matrix R = mat1.times(mat2);
			// the difference vector as a one-dimensional matrix
			Matrix vB = new Matrix(new double[]{vx[i] - vx[i-1], vy[i] - vy[i-1], vz[i] - vz[i-1]}, 1);
			Matrix vB_rot = R.transpose().times(vB.transpose());  //  3x3 times 3x1, hence the transposing of the 1x3 vector so that the inner lengths are the same
			double[][] arr = vB_rot.getArray();
			rvx[i] = arr[0][0];
			rvy[i] = arr[1][0];
			rvz[i] = arr[2][0];
			//Utils.log2("rv{x,y,z}[" + i + "]= " + rvx[i] + ", " + rvy[i] + ", " + rvz[i]);


			/*
			Vector3D ref = new Vector3D(1,0,0); // WARNING with a 1,1,1 and then normalize() the results are very different (?)
			Vector3D v1 = new Vector3D(vx[i-1], vy[i-1], vz[i-1]);
			v1.normalize();
			Vector3D v2 = new Vector3D(vx[i] - vx[i-1], vy[i] - vy[i-1], vz[i] - vz[i-1]); // the vector of differences
			v2.normalize();
			Vector3D axis = ref.crossProduct(v1);
			double angle = Math.acos(ref.dotProduct(v1));
			double sin = Math.sin(-angle); // rotate minus the angle
			double cos = Math.cos(-angle);
			Vector3D r = v2.rotateAroundAxis(axis, sin, cos); // will normalize axis
			r.normalize();
			r.scale(delta);
			rvx[i] = r.x;
			rvy[i] = r.y;
			rvz[i] = r.z;

			*/

			//check:
			//r.normalize();
			// angles are identical! GOOD!// Utils.log2("Angle before: " + Math.acos(v1.dotProduct(v2)) + " Angle after:" + Math.acos(ref.dotProduct(r)));

			// even easier: why not just the angle between the consecutive normalized vectors?
			// NO: because the vector can pivot in 3D, keeping the same angle but being different


		}
		Utils.log2("relative used delta " + delta);
	}

	/** Expand or shrink the points in this 3D path so that the average point interdistance becomes delta.
	 *
	 * This method is intended to enhance the comparison of two paths in 3D space which may be similar but differ greatly in dimensions; for example, secondary lineage tracts in 3rd instar Drosophila larvae versus adult.
	 */
	public void equalize(final double target_delta) {
		if (length < 2) return;
		final double scale = target_delta / getAverageDelta();
		for (int i=0; i<length; i++) {
			x[i] *= scale;
			y[i] *= scale;
			z[i] *= scale;
		}
	}

	static public VectorString3D createRandom(final int length, final double delta, final boolean closed) throws Exception {
		Utils.log2("Creating random with length " + length + " and delta " + delta);
		double[] x = new double[length];
		double[] y = new double[length];
		double[] z = new double[length];
		Random rand = new Random(69997); // fixed seed, so that when length is equal the generated sequence is also equal
		Vector v = new Vector(0,0,0);
		for (int i=0; i<length; i++) {
			v.set(rand.nextDouble()*2 -1, rand.nextDouble()*2 -1, rand.nextDouble()*2 -1); // random values in the range [-1,1]
			v.setLength(delta);
			v.put(i, x, y, z);
		}
		final VectorString3D vs = new VectorString3D(x, y, z, closed);
		vs.delta = delta;
		vs.vx = new double[length];
		vs.vy = new double[length];
		vs.vz = new double[length];
		for (int i=1; i<length; i++) {
			vs.vx[i] = vs.x[i] - vs.x[i-1];
			vs.vy[i] = vs.y[i] - vs.y[i-1];
			vs.vz[i] = vs.z[i] - vs.z[i-1];
		}
		if (closed) {
			vs.vx[0] = vs.x[0] - vs.x[length-1];
			vs.vy[0] = vs.y[0] - vs.y[length-1];
			vs.vz[0] = vs.z[0] - vs.z[length-1];
		}
		return vs;
	}

	/** Scale to match cal.pixelWidth, cal.pixelHeight and cal.pixelDepth. If cal is null, returns immediately. Will make all vectors null, so you must call resample(delta) again after calibrating. */
	public void calibrate(final Calibration cal) {
		if (null == cal) return;
		this.cal = cal;
		for (int i=0; i<x.length; i++) {
			x[i] *= cal.pixelWidth;
			y[i] *= cal.pixelHeight;
			// TODO z is obtained from the layer, which is already set in pixel coordinates // z[i] *= cal.pixelDepth;
			// That has to change eventually.
		}
		// reset vectors
		vx = vy = vz = null;
		rvx = rvy = rvz = null;
		delta = 0;
	}

	public boolean isCalibrated() {
		return null != this.cal;
	}
	public Calibration getCalibrationCopy() {
		return null == this.cal ? null : this.cal.copy();
	}
}