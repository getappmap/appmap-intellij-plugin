import { require_dist, require_dayjs_min, require_dist2, require_purify, getConfig, getAccTitle, setAccTitle, getAccDescription, setAccDescription, getDiagramTitle, setDiagramTitle, clear, common$1, defaultConfig, select_default, ordinal, Tableau10_default, setupGraphViewbox$1 } from './chunk-YWHJFWTB.js';
import { __commonJS, init_polyfillShim, __toESM } from './chunk-NBJJPFWB.js';

// node_modules/d3-sankey/node_modules/d3-array/dist/d3-array.js
var require_d3_array = __commonJS({
  "node_modules/d3-sankey/node_modules/d3-array/dist/d3-array.js"(exports, module) {
    init_polyfillShim();
    (function(global, factory) {
      typeof exports === "object" && typeof module !== "undefined" ? factory(exports) : typeof define === "function" && define.amd ? define(["exports"], factory) : (global = typeof globalThis !== "undefined" ? globalThis : global || self, factory(global.d3 = global.d3 || {}));
    })(exports, function(exports2) {
      function ascending(a, b) {
        return a < b ? -1 : a > b ? 1 : a >= b ? 0 : NaN;
      }
      function bisector(f) {
        let delta = f;
        let compare = f;
        if (f.length === 1) {
          delta = (d, x) => f(d) - x;
          compare = ascendingComparator(f);
        }
        function left(a, x, lo, hi) {
          if (lo == null) lo = 0;
          if (hi == null) hi = a.length;
          while (lo < hi) {
            const mid = lo + hi >>> 1;
            if (compare(a[mid], x) < 0) lo = mid + 1;
            else hi = mid;
          }
          return lo;
        }
        function right(a, x, lo, hi) {
          if (lo == null) lo = 0;
          if (hi == null) hi = a.length;
          while (lo < hi) {
            const mid = lo + hi >>> 1;
            if (compare(a[mid], x) > 0) hi = mid;
            else lo = mid + 1;
          }
          return lo;
        }
        function center(a, x, lo, hi) {
          if (lo == null) lo = 0;
          if (hi == null) hi = a.length;
          const i = left(a, x, lo, hi - 1);
          return i > lo && delta(a[i - 1], x) > -delta(a[i], x) ? i - 1 : i;
        }
        return { left, center, right };
      }
      function ascendingComparator(f) {
        return (d, x) => ascending(f(d), x);
      }
      function number(x) {
        return x === null ? NaN : +x;
      }
      function* numbers(values, valueof) {
        if (valueof === void 0) {
          for (let value of values) {
            if (value != null && (value = +value) >= value) {
              yield value;
            }
          }
        } else {
          let index2 = -1;
          for (let value of values) {
            if ((value = valueof(value, ++index2, values)) != null && (value = +value) >= value) {
              yield value;
            }
          }
        }
      }
      const ascendingBisect = bisector(ascending);
      const bisectRight = ascendingBisect.right;
      const bisectLeft = ascendingBisect.left;
      const bisectCenter = bisector(number).center;
      function count(values, valueof) {
        let count2 = 0;
        if (valueof === void 0) {
          for (let value of values) {
            if (value != null && (value = +value) >= value) {
              ++count2;
            }
          }
        } else {
          let index2 = -1;
          for (let value of values) {
            if ((value = valueof(value, ++index2, values)) != null && (value = +value) >= value) {
              ++count2;
            }
          }
        }
        return count2;
      }
      function length$1(array2) {
        return array2.length | 0;
      }
      function empty(length2) {
        return !(length2 > 0);
      }
      function arrayify(values) {
        return typeof values !== "object" || "length" in values ? values : Array.from(values);
      }
      function reducer(reduce2) {
        return (values) => reduce2(...values);
      }
      function cross(...values) {
        const reduce2 = typeof values[values.length - 1] === "function" && reducer(values.pop());
        values = values.map(arrayify);
        const lengths = values.map(length$1);
        const j = values.length - 1;
        const index2 = new Array(j + 1).fill(0);
        const product = [];
        if (j < 0 || lengths.some(empty)) return product;
        while (true) {
          product.push(index2.map((j2, i2) => values[i2][j2]));
          let i = j;
          while (++index2[i] === lengths[i]) {
            if (i === 0) return reduce2 ? product.map(reduce2) : product;
            index2[i--] = 0;
          }
        }
      }
      function cumsum(values, valueof) {
        var sum2 = 0, index2 = 0;
        return Float64Array.from(values, valueof === void 0 ? (v) => sum2 += +v || 0 : (v) => sum2 += +valueof(v, index2++, values) || 0);
      }
      function descending(a, b) {
        return b < a ? -1 : b > a ? 1 : b >= a ? 0 : NaN;
      }
      function variance(values, valueof) {
        let count2 = 0;
        let delta;
        let mean2 = 0;
        let sum2 = 0;
        if (valueof === void 0) {
          for (let value of values) {
            if (value != null && (value = +value) >= value) {
              delta = value - mean2;
              mean2 += delta / ++count2;
              sum2 += delta * (value - mean2);
            }
          }
        } else {
          let index2 = -1;
          for (let value of values) {
            if ((value = valueof(value, ++index2, values)) != null && (value = +value) >= value) {
              delta = value - mean2;
              mean2 += delta / ++count2;
              sum2 += delta * (value - mean2);
            }
          }
        }
        if (count2 > 1) return sum2 / (count2 - 1);
      }
      function deviation(values, valueof) {
        const v = variance(values, valueof);
        return v ? Math.sqrt(v) : v;
      }
      function extent(values, valueof) {
        let min2;
        let max2;
        if (valueof === void 0) {
          for (const value of values) {
            if (value != null) {
              if (min2 === void 0) {
                if (value >= value) min2 = max2 = value;
              } else {
                if (min2 > value) min2 = value;
                if (max2 < value) max2 = value;
              }
            }
          }
        } else {
          let index2 = -1;
          for (let value of values) {
            if ((value = valueof(value, ++index2, values)) != null) {
              if (min2 === void 0) {
                if (value >= value) min2 = max2 = value;
              } else {
                if (min2 > value) min2 = value;
                if (max2 < value) max2 = value;
              }
            }
          }
        }
        return [min2, max2];
      }
      class Adder {
        constructor() {
          this._partials = new Float64Array(32);
          this._n = 0;
        }
        add(x) {
          const p = this._partials;
          let i = 0;
          for (let j = 0; j < this._n && j < 32; j++) {
            const y = p[j], hi = x + y, lo = Math.abs(x) < Math.abs(y) ? x - (hi - y) : y - (hi - x);
            if (lo) p[i++] = lo;
            x = hi;
          }
          p[i] = x;
          this._n = i + 1;
          return this;
        }
        valueOf() {
          const p = this._partials;
          let n = this._n, x, y, lo, hi = 0;
          if (n > 0) {
            hi = p[--n];
            while (n > 0) {
              x = hi;
              y = p[--n];
              hi = x + y;
              lo = y - (hi - x);
              if (lo) break;
            }
            if (n > 0 && (lo < 0 && p[n - 1] < 0 || lo > 0 && p[n - 1] > 0)) {
              y = lo * 2;
              x = hi + y;
              if (y == x - hi) hi = x;
            }
          }
          return hi;
        }
      }
      function fsum(values, valueof) {
        const adder = new Adder();
        if (valueof === void 0) {
          for (let value of values) {
            if (value = +value) {
              adder.add(value);
            }
          }
        } else {
          let index2 = -1;
          for (let value of values) {
            if (value = +valueof(value, ++index2, values)) {
              adder.add(value);
            }
          }
        }
        return +adder;
      }
      function fcumsum(values, valueof) {
        const adder = new Adder();
        let index2 = -1;
        return Float64Array.from(
          values,
          valueof === void 0 ? (v) => adder.add(+v || 0) : (v) => adder.add(+valueof(v, ++index2, values) || 0)
        );
      }
      class InternMap extends Map {
        constructor(entries, key = keyof) {
          super();
          Object.defineProperties(this, { _intern: { value: /* @__PURE__ */ new Map() }, _key: { value: key } });
          if (entries != null) for (const [key2, value] of entries) this.set(key2, value);
        }
        get(key) {
          return super.get(intern_get(this, key));
        }
        has(key) {
          return super.has(intern_get(this, key));
        }
        set(key, value) {
          return super.set(intern_set(this, key), value);
        }
        delete(key) {
          return super.delete(intern_delete(this, key));
        }
      }
      class InternSet extends Set {
        constructor(values, key = keyof) {
          super();
          Object.defineProperties(this, { _intern: { value: /* @__PURE__ */ new Map() }, _key: { value: key } });
          if (values != null) for (const value of values) this.add(value);
        }
        has(value) {
          return super.has(intern_get(this, value));
        }
        add(value) {
          return super.add(intern_set(this, value));
        }
        delete(value) {
          return super.delete(intern_delete(this, value));
        }
      }
      function intern_get({ _intern, _key }, value) {
        const key = _key(value);
        return _intern.has(key) ? _intern.get(key) : value;
      }
      function intern_set({ _intern, _key }, value) {
        const key = _key(value);
        if (_intern.has(key)) return _intern.get(key);
        _intern.set(key, value);
        return value;
      }
      function intern_delete({ _intern, _key }, value) {
        const key = _key(value);
        if (_intern.has(key)) {
          value = _intern.get(value);
          _intern.delete(key);
        }
        return value;
      }
      function keyof(value) {
        return value !== null && typeof value === "object" ? value.valueOf() : value;
      }
      function identity(x) {
        return x;
      }
      function group(values, ...keys) {
        return nest(values, identity, identity, keys);
      }
      function groups(values, ...keys) {
        return nest(values, Array.from, identity, keys);
      }
      function rollup(values, reduce2, ...keys) {
        return nest(values, identity, reduce2, keys);
      }
      function rollups(values, reduce2, ...keys) {
        return nest(values, Array.from, reduce2, keys);
      }
      function index(values, ...keys) {
        return nest(values, identity, unique, keys);
      }
      function indexes(values, ...keys) {
        return nest(values, Array.from, unique, keys);
      }
      function unique(values) {
        if (values.length !== 1) throw new Error("duplicate key");
        return values[0];
      }
      function nest(values, map2, reduce2, keys) {
        return function regroup(values2, i) {
          if (i >= keys.length) return reduce2(values2);
          const groups2 = new InternMap();
          const keyof2 = keys[i++];
          let index2 = -1;
          for (const value of values2) {
            const key = keyof2(value, ++index2, values2);
            const group2 = groups2.get(key);
            if (group2) group2.push(value);
            else groups2.set(key, [value]);
          }
          for (const [key, values3] of groups2) {
            groups2.set(key, regroup(values3, i));
          }
          return map2(groups2);
        }(values, 0);
      }
      function permute(source, keys) {
        return Array.from(keys, (key) => source[key]);
      }
      function sort(values, ...F) {
        if (typeof values[Symbol.iterator] !== "function") throw new TypeError("values is not iterable");
        values = Array.from(values);
        let [f = ascending] = F;
        if (f.length === 1 || F.length > 1) {
          const index2 = Uint32Array.from(values, (d, i) => i);
          if (F.length > 1) {
            F = F.map((f2) => values.map(f2));
            index2.sort((i, j) => {
              for (const f2 of F) {
                const c = ascending(f2[i], f2[j]);
                if (c) return c;
              }
            });
          } else {
            f = values.map(f);
            index2.sort((i, j) => ascending(f[i], f[j]));
          }
          return permute(values, index2);
        }
        return values.sort(f);
      }
      function groupSort(values, reduce2, key) {
        return (reduce2.length === 1 ? sort(rollup(values, reduce2, key), ([ak, av], [bk, bv]) => ascending(av, bv) || ascending(ak, bk)) : sort(group(values, key), ([ak, av], [bk, bv]) => reduce2(av, bv) || ascending(ak, bk))).map(([key2]) => key2);
      }
      var array = Array.prototype;
      var slice = array.slice;
      function constant(x) {
        return function() {
          return x;
        };
      }
      var e10 = Math.sqrt(50), e5 = Math.sqrt(10), e2 = Math.sqrt(2);
      function ticks(start, stop, count2) {
        var reverse2, i = -1, n, ticks2, step;
        stop = +stop, start = +start, count2 = +count2;
        if (start === stop && count2 > 0) return [start];
        if (reverse2 = stop < start) n = start, start = stop, stop = n;
        if ((step = tickIncrement(start, stop, count2)) === 0 || !isFinite(step)) return [];
        if (step > 0) {
          let r0 = Math.round(start / step), r1 = Math.round(stop / step);
          if (r0 * step < start) ++r0;
          if (r1 * step > stop) --r1;
          ticks2 = new Array(n = r1 - r0 + 1);
          while (++i < n) ticks2[i] = (r0 + i) * step;
        } else {
          step = -step;
          let r0 = Math.round(start * step), r1 = Math.round(stop * step);
          if (r0 / step < start) ++r0;
          if (r1 / step > stop) --r1;
          ticks2 = new Array(n = r1 - r0 + 1);
          while (++i < n) ticks2[i] = (r0 + i) / step;
        }
        if (reverse2) ticks2.reverse();
        return ticks2;
      }
      function tickIncrement(start, stop, count2) {
        var step = (stop - start) / Math.max(0, count2), power = Math.floor(Math.log(step) / Math.LN10), error = step / Math.pow(10, power);
        return power >= 0 ? (error >= e10 ? 10 : error >= e5 ? 5 : error >= e2 ? 2 : 1) * Math.pow(10, power) : -Math.pow(10, -power) / (error >= e10 ? 10 : error >= e5 ? 5 : error >= e2 ? 2 : 1);
      }
      function tickStep(start, stop, count2) {
        var step0 = Math.abs(stop - start) / Math.max(0, count2), step1 = Math.pow(10, Math.floor(Math.log(step0) / Math.LN10)), error = step0 / step1;
        if (error >= e10) step1 *= 10;
        else if (error >= e5) step1 *= 5;
        else if (error >= e2) step1 *= 2;
        return stop < start ? -step1 : step1;
      }
      function nice(start, stop, count2) {
        let prestep;
        while (true) {
          const step = tickIncrement(start, stop, count2);
          if (step === prestep || step === 0 || !isFinite(step)) {
            return [start, stop];
          } else if (step > 0) {
            start = Math.floor(start / step) * step;
            stop = Math.ceil(stop / step) * step;
          } else if (step < 0) {
            start = Math.ceil(start * step) / step;
            stop = Math.floor(stop * step) / step;
          }
          prestep = step;
        }
      }
      function sturges(values) {
        return Math.ceil(Math.log(count(values)) / Math.LN2) + 1;
      }
      function bin() {
        var value = identity, domain = extent, threshold = sturges;
        function histogram(data) {
          if (!Array.isArray(data)) data = Array.from(data);
          var i, n = data.length, x, values = new Array(n);
          for (i = 0; i < n; ++i) {
            values[i] = value(data[i], i, data);
          }
          var xz = domain(values), x0 = xz[0], x1 = xz[1], tz = threshold(values, x0, x1);
          if (!Array.isArray(tz)) {
            const max2 = x1, tn = +tz;
            if (domain === extent) [x0, x1] = nice(x0, x1, tn);
            tz = ticks(x0, x1, tn);
            if (tz[tz.length - 1] >= x1) {
              if (max2 >= x1 && domain === extent) {
                const step = tickIncrement(x0, x1, tn);
                if (isFinite(step)) {
                  if (step > 0) {
                    x1 = (Math.floor(x1 / step) + 1) * step;
                  } else if (step < 0) {
                    x1 = (Math.ceil(x1 * -step) + 1) / -step;
                  }
                }
              } else {
                tz.pop();
              }
            }
          }
          var m = tz.length;
          while (tz[0] <= x0) tz.shift(), --m;
          while (tz[m - 1] > x1) tz.pop(), --m;
          var bins = new Array(m + 1), bin2;
          for (i = 0; i <= m; ++i) {
            bin2 = bins[i] = [];
            bin2.x0 = i > 0 ? tz[i - 1] : x0;
            bin2.x1 = i < m ? tz[i] : x1;
          }
          for (i = 0; i < n; ++i) {
            x = values[i];
            if (x0 <= x && x <= x1) {
              bins[bisectRight(tz, x, 0, m)].push(data[i]);
            }
          }
          return bins;
        }
        histogram.value = function(_) {
          return arguments.length ? (value = typeof _ === "function" ? _ : constant(_), histogram) : value;
        };
        histogram.domain = function(_) {
          return arguments.length ? (domain = typeof _ === "function" ? _ : constant([_[0], _[1]]), histogram) : domain;
        };
        histogram.thresholds = function(_) {
          return arguments.length ? (threshold = typeof _ === "function" ? _ : Array.isArray(_) ? constant(slice.call(_)) : constant(_), histogram) : threshold;
        };
        return histogram;
      }
      function max(values, valueof) {
        let max2;
        if (valueof === void 0) {
          for (const value of values) {
            if (value != null && (max2 < value || max2 === void 0 && value >= value)) {
              max2 = value;
            }
          }
        } else {
          let index2 = -1;
          for (let value of values) {
            if ((value = valueof(value, ++index2, values)) != null && (max2 < value || max2 === void 0 && value >= value)) {
              max2 = value;
            }
          }
        }
        return max2;
      }
      function min(values, valueof) {
        let min2;
        if (valueof === void 0) {
          for (const value of values) {
            if (value != null && (min2 > value || min2 === void 0 && value >= value)) {
              min2 = value;
            }
          }
        } else {
          let index2 = -1;
          for (let value of values) {
            if ((value = valueof(value, ++index2, values)) != null && (min2 > value || min2 === void 0 && value >= value)) {
              min2 = value;
            }
          }
        }
        return min2;
      }
      function quickselect(array2, k, left = 0, right = array2.length - 1, compare = ascending) {
        while (right > left) {
          if (right - left > 600) {
            const n = right - left + 1;
            const m = k - left + 1;
            const z = Math.log(n);
            const s = 0.5 * Math.exp(2 * z / 3);
            const sd = 0.5 * Math.sqrt(z * s * (n - s) / n) * (m - n / 2 < 0 ? -1 : 1);
            const newLeft = Math.max(left, Math.floor(k - m * s / n + sd));
            const newRight = Math.min(right, Math.floor(k + (n - m) * s / n + sd));
            quickselect(array2, k, newLeft, newRight, compare);
          }
          const t = array2[k];
          let i = left;
          let j = right;
          swap(array2, left, k);
          if (compare(array2[right], t) > 0) swap(array2, left, right);
          while (i < j) {
            swap(array2, i, j), ++i, --j;
            while (compare(array2[i], t) < 0) ++i;
            while (compare(array2[j], t) > 0) --j;
          }
          if (compare(array2[left], t) === 0) swap(array2, left, j);
          else ++j, swap(array2, j, right);
          if (j <= k) left = j + 1;
          if (k <= j) right = j - 1;
        }
        return array2;
      }
      function swap(array2, i, j) {
        const t = array2[i];
        array2[i] = array2[j];
        array2[j] = t;
      }
      function quantile(values, p, valueof) {
        values = Float64Array.from(numbers(values, valueof));
        if (!(n = values.length)) return;
        if ((p = +p) <= 0 || n < 2) return min(values);
        if (p >= 1) return max(values);
        var n, i = (n - 1) * p, i0 = Math.floor(i), value0 = max(quickselect(values, i0).subarray(0, i0 + 1)), value1 = min(values.subarray(i0 + 1));
        return value0 + (value1 - value0) * (i - i0);
      }
      function quantileSorted(values, p, valueof = number) {
        if (!(n = values.length)) return;
        if ((p = +p) <= 0 || n < 2) return +valueof(values[0], 0, values);
        if (p >= 1) return +valueof(values[n - 1], n - 1, values);
        var n, i = (n - 1) * p, i0 = Math.floor(i), value0 = +valueof(values[i0], i0, values), value1 = +valueof(values[i0 + 1], i0 + 1, values);
        return value0 + (value1 - value0) * (i - i0);
      }
      function freedmanDiaconis(values, min2, max2) {
        return Math.ceil((max2 - min2) / (2 * (quantile(values, 0.75) - quantile(values, 0.25)) * Math.pow(count(values), -1 / 3)));
      }
      function scott(values, min2, max2) {
        return Math.ceil((max2 - min2) / (3.5 * deviation(values) * Math.pow(count(values), -1 / 3)));
      }
      function maxIndex(values, valueof) {
        let max2;
        let maxIndex2 = -1;
        let index2 = -1;
        if (valueof === void 0) {
          for (const value of values) {
            ++index2;
            if (value != null && (max2 < value || max2 === void 0 && value >= value)) {
              max2 = value, maxIndex2 = index2;
            }
          }
        } else {
          for (let value of values) {
            if ((value = valueof(value, ++index2, values)) != null && (max2 < value || max2 === void 0 && value >= value)) {
              max2 = value, maxIndex2 = index2;
            }
          }
        }
        return maxIndex2;
      }
      function mean(values, valueof) {
        let count2 = 0;
        let sum2 = 0;
        if (valueof === void 0) {
          for (let value of values) {
            if (value != null && (value = +value) >= value) {
              ++count2, sum2 += value;
            }
          }
        } else {
          let index2 = -1;
          for (let value of values) {
            if ((value = valueof(value, ++index2, values)) != null && (value = +value) >= value) {
              ++count2, sum2 += value;
            }
          }
        }
        if (count2) return sum2 / count2;
      }
      function median(values, valueof) {
        return quantile(values, 0.5, valueof);
      }
      function* flatten(arrays) {
        for (const array2 of arrays) {
          yield* array2;
        }
      }
      function merge(arrays) {
        return Array.from(flatten(arrays));
      }
      function minIndex(values, valueof) {
        let min2;
        let minIndex2 = -1;
        let index2 = -1;
        if (valueof === void 0) {
          for (const value of values) {
            ++index2;
            if (value != null && (min2 > value || min2 === void 0 && value >= value)) {
              min2 = value, minIndex2 = index2;
            }
          }
        } else {
          for (let value of values) {
            if ((value = valueof(value, ++index2, values)) != null && (min2 > value || min2 === void 0 && value >= value)) {
              min2 = value, minIndex2 = index2;
            }
          }
        }
        return minIndex2;
      }
      function pairs(values, pairof = pair) {
        const pairs2 = [];
        let previous;
        let first = false;
        for (const value of values) {
          if (first) pairs2.push(pairof(previous, value));
          previous = value;
          first = true;
        }
        return pairs2;
      }
      function pair(a, b) {
        return [a, b];
      }
      function range(start, stop, step) {
        start = +start, stop = +stop, step = (n = arguments.length) < 2 ? (stop = start, start = 0, 1) : n < 3 ? 1 : +step;
        var i = -1, n = Math.max(0, Math.ceil((stop - start) / step)) | 0, range2 = new Array(n);
        while (++i < n) {
          range2[i] = start + i * step;
        }
        return range2;
      }
      function least(values, compare = ascending) {
        let min2;
        let defined = false;
        if (compare.length === 1) {
          let minValue;
          for (const element of values) {
            const value = compare(element);
            if (defined ? ascending(value, minValue) < 0 : ascending(value, value) === 0) {
              min2 = element;
              minValue = value;
              defined = true;
            }
          }
        } else {
          for (const value of values) {
            if (defined ? compare(value, min2) < 0 : compare(value, value) === 0) {
              min2 = value;
              defined = true;
            }
          }
        }
        return min2;
      }
      function leastIndex(values, compare = ascending) {
        if (compare.length === 1) return minIndex(values, compare);
        let minValue;
        let min2 = -1;
        let index2 = -1;
        for (const value of values) {
          ++index2;
          if (min2 < 0 ? compare(value, value) === 0 : compare(value, minValue) < 0) {
            minValue = value;
            min2 = index2;
          }
        }
        return min2;
      }
      function greatest(values, compare = ascending) {
        let max2;
        let defined = false;
        if (compare.length === 1) {
          let maxValue;
          for (const element of values) {
            const value = compare(element);
            if (defined ? ascending(value, maxValue) > 0 : ascending(value, value) === 0) {
              max2 = element;
              maxValue = value;
              defined = true;
            }
          }
        } else {
          for (const value of values) {
            if (defined ? compare(value, max2) > 0 : compare(value, value) === 0) {
              max2 = value;
              defined = true;
            }
          }
        }
        return max2;
      }
      function greatestIndex(values, compare = ascending) {
        if (compare.length === 1) return maxIndex(values, compare);
        let maxValue;
        let max2 = -1;
        let index2 = -1;
        for (const value of values) {
          ++index2;
          if (max2 < 0 ? compare(value, value) === 0 : compare(value, maxValue) > 0) {
            maxValue = value;
            max2 = index2;
          }
        }
        return max2;
      }
      function scan(values, compare) {
        const index2 = leastIndex(values, compare);
        return index2 < 0 ? void 0 : index2;
      }
      var shuffle = shuffler(Math.random);
      function shuffler(random) {
        return function shuffle2(array2, i0 = 0, i1 = array2.length) {
          let m = i1 - (i0 = +i0);
          while (m) {
            const i = random() * m-- | 0, t = array2[m + i0];
            array2[m + i0] = array2[i + i0];
            array2[i + i0] = t;
          }
          return array2;
        };
      }
      function sum(values, valueof) {
        let sum2 = 0;
        if (valueof === void 0) {
          for (let value of values) {
            if (value = +value) {
              sum2 += value;
            }
          }
        } else {
          let index2 = -1;
          for (let value of values) {
            if (value = +valueof(value, ++index2, values)) {
              sum2 += value;
            }
          }
        }
        return sum2;
      }
      function transpose(matrix) {
        if (!(n = matrix.length)) return [];
        for (var i = -1, m = min(matrix, length), transpose2 = new Array(m); ++i < m; ) {
          for (var j = -1, n, row = transpose2[i] = new Array(n); ++j < n; ) {
            row[j] = matrix[j][i];
          }
        }
        return transpose2;
      }
      function length(d) {
        return d.length;
      }
      function zip() {
        return transpose(arguments);
      }
      function every(values, test) {
        if (typeof test !== "function") throw new TypeError("test is not a function");
        let index2 = -1;
        for (const value of values) {
          if (!test(value, ++index2, values)) {
            return false;
          }
        }
        return true;
      }
      function some(values, test) {
        if (typeof test !== "function") throw new TypeError("test is not a function");
        let index2 = -1;
        for (const value of values) {
          if (test(value, ++index2, values)) {
            return true;
          }
        }
        return false;
      }
      function filter(values, test) {
        if (typeof test !== "function") throw new TypeError("test is not a function");
        const array2 = [];
        let index2 = -1;
        for (const value of values) {
          if (test(value, ++index2, values)) {
            array2.push(value);
          }
        }
        return array2;
      }
      function map(values, mapper) {
        if (typeof values[Symbol.iterator] !== "function") throw new TypeError("values is not iterable");
        if (typeof mapper !== "function") throw new TypeError("mapper is not a function");
        return Array.from(values, (value, index2) => mapper(value, index2, values));
      }
      function reduce(values, reducer2, value) {
        if (typeof reducer2 !== "function") throw new TypeError("reducer is not a function");
        const iterator = values[Symbol.iterator]();
        let done, next, index2 = -1;
        if (arguments.length < 3) {
          ({ done, value } = iterator.next());
          if (done) return;
          ++index2;
        }
        while ({ done, value: next } = iterator.next(), !done) {
          value = reducer2(value, next, ++index2, values);
        }
        return value;
      }
      function reverse(values) {
        if (typeof values[Symbol.iterator] !== "function") throw new TypeError("values is not iterable");
        return Array.from(values).reverse();
      }
      function difference(values, ...others) {
        values = new Set(values);
        for (const other of others) {
          for (const value of other) {
            values.delete(value);
          }
        }
        return values;
      }
      function disjoint(values, other) {
        const iterator = other[Symbol.iterator](), set2 = /* @__PURE__ */ new Set();
        for (const v of values) {
          if (set2.has(v)) return false;
          let value, done;
          while ({ value, done } = iterator.next()) {
            if (done) break;
            if (Object.is(v, value)) return false;
            set2.add(value);
          }
        }
        return true;
      }
      function set(values) {
        return values instanceof Set ? values : new Set(values);
      }
      function intersection(values, ...others) {
        values = new Set(values);
        others = others.map(set);
        out: for (const value of values) {
          for (const other of others) {
            if (!other.has(value)) {
              values.delete(value);
              continue out;
            }
          }
        }
        return values;
      }
      function superset(values, other) {
        const iterator = values[Symbol.iterator](), set2 = /* @__PURE__ */ new Set();
        for (const o of other) {
          if (set2.has(o)) continue;
          let value, done;
          while ({ value, done } = iterator.next()) {
            if (done) return false;
            set2.add(value);
            if (Object.is(o, value)) break;
          }
        }
        return true;
      }
      function subset(values, other) {
        return superset(other, values);
      }
      function union(...others) {
        const set2 = /* @__PURE__ */ new Set();
        for (const other of others) {
          for (const o of other) {
            set2.add(o);
          }
        }
        return set2;
      }
      exports2.Adder = Adder;
      exports2.InternMap = InternMap;
      exports2.InternSet = InternSet;
      exports2.ascending = ascending;
      exports2.bin = bin;
      exports2.bisect = bisectRight;
      exports2.bisectCenter = bisectCenter;
      exports2.bisectLeft = bisectLeft;
      exports2.bisectRight = bisectRight;
      exports2.bisector = bisector;
      exports2.count = count;
      exports2.cross = cross;
      exports2.cumsum = cumsum;
      exports2.descending = descending;
      exports2.deviation = deviation;
      exports2.difference = difference;
      exports2.disjoint = disjoint;
      exports2.every = every;
      exports2.extent = extent;
      exports2.fcumsum = fcumsum;
      exports2.filter = filter;
      exports2.fsum = fsum;
      exports2.greatest = greatest;
      exports2.greatestIndex = greatestIndex;
      exports2.group = group;
      exports2.groupSort = groupSort;
      exports2.groups = groups;
      exports2.histogram = bin;
      exports2.index = index;
      exports2.indexes = indexes;
      exports2.intersection = intersection;
      exports2.least = least;
      exports2.leastIndex = leastIndex;
      exports2.map = map;
      exports2.max = max;
      exports2.maxIndex = maxIndex;
      exports2.mean = mean;
      exports2.median = median;
      exports2.merge = merge;
      exports2.min = min;
      exports2.minIndex = minIndex;
      exports2.nice = nice;
      exports2.pairs = pairs;
      exports2.permute = permute;
      exports2.quantile = quantile;
      exports2.quantileSorted = quantileSorted;
      exports2.quickselect = quickselect;
      exports2.range = range;
      exports2.reduce = reduce;
      exports2.reverse = reverse;
      exports2.rollup = rollup;
      exports2.rollups = rollups;
      exports2.scan = scan;
      exports2.shuffle = shuffle;
      exports2.shuffler = shuffler;
      exports2.some = some;
      exports2.sort = sort;
      exports2.subset = subset;
      exports2.sum = sum;
      exports2.superset = superset;
      exports2.thresholdFreedmanDiaconis = freedmanDiaconis;
      exports2.thresholdScott = scott;
      exports2.thresholdSturges = sturges;
      exports2.tickIncrement = tickIncrement;
      exports2.tickStep = tickStep;
      exports2.ticks = ticks;
      exports2.transpose = transpose;
      exports2.union = union;
      exports2.variance = variance;
      exports2.zip = zip;
      Object.defineProperty(exports2, "__esModule", { value: true });
    });
  }
});

// node_modules/d3-sankey/node_modules/d3-path/dist/d3-path.js
var require_d3_path = __commonJS({
  "node_modules/d3-sankey/node_modules/d3-path/dist/d3-path.js"(exports, module) {
    init_polyfillShim();
    (function(global, factory) {
      typeof exports === "object" && typeof module !== "undefined" ? factory(exports) : typeof define === "function" && define.amd ? define(["exports"], factory) : (global = global || self, factory(global.d3 = global.d3 || {}));
    })(exports, function(exports2) {
      var pi = Math.PI, tau = 2 * pi, epsilon = 1e-6, tauEpsilon = tau - epsilon;
      function Path() {
        this._x0 = this._y0 = // start of current subpath
        this._x1 = this._y1 = null;
        this._ = "";
      }
      function path() {
        return new Path();
      }
      Path.prototype = path.prototype = {
        constructor: Path,
        moveTo: function(x, y) {
          this._ += "M" + (this._x0 = this._x1 = +x) + "," + (this._y0 = this._y1 = +y);
        },
        closePath: function() {
          if (this._x1 !== null) {
            this._x1 = this._x0, this._y1 = this._y0;
            this._ += "Z";
          }
        },
        lineTo: function(x, y) {
          this._ += "L" + (this._x1 = +x) + "," + (this._y1 = +y);
        },
        quadraticCurveTo: function(x1, y1, x, y) {
          this._ += "Q" + +x1 + "," + +y1 + "," + (this._x1 = +x) + "," + (this._y1 = +y);
        },
        bezierCurveTo: function(x1, y1, x2, y2, x, y) {
          this._ += "C" + +x1 + "," + +y1 + "," + +x2 + "," + +y2 + "," + (this._x1 = +x) + "," + (this._y1 = +y);
        },
        arcTo: function(x1, y1, x2, y2, r) {
          x1 = +x1, y1 = +y1, x2 = +x2, y2 = +y2, r = +r;
          var x0 = this._x1, y0 = this._y1, x21 = x2 - x1, y21 = y2 - y1, x01 = x0 - x1, y01 = y0 - y1, l01_2 = x01 * x01 + y01 * y01;
          if (r < 0) throw new Error("negative radius: " + r);
          if (this._x1 === null) {
            this._ += "M" + (this._x1 = x1) + "," + (this._y1 = y1);
          } else if (!(l01_2 > epsilon)) ;
          else if (!(Math.abs(y01 * x21 - y21 * x01) > epsilon) || !r) {
            this._ += "L" + (this._x1 = x1) + "," + (this._y1 = y1);
          } else {
            var x20 = x2 - x0, y20 = y2 - y0, l21_2 = x21 * x21 + y21 * y21, l20_2 = x20 * x20 + y20 * y20, l21 = Math.sqrt(l21_2), l01 = Math.sqrt(l01_2), l = r * Math.tan((pi - Math.acos((l21_2 + l01_2 - l20_2) / (2 * l21 * l01))) / 2), t01 = l / l01, t21 = l / l21;
            if (Math.abs(t01 - 1) > epsilon) {
              this._ += "L" + (x1 + t01 * x01) + "," + (y1 + t01 * y01);
            }
            this._ += "A" + r + "," + r + ",0,0," + +(y01 * x20 > x01 * y20) + "," + (this._x1 = x1 + t21 * x21) + "," + (this._y1 = y1 + t21 * y21);
          }
        },
        arc: function(x, y, r, a0, a1, ccw) {
          x = +x, y = +y, r = +r, ccw = !!ccw;
          var dx = r * Math.cos(a0), dy = r * Math.sin(a0), x0 = x + dx, y0 = y + dy, cw = 1 ^ ccw, da = ccw ? a0 - a1 : a1 - a0;
          if (r < 0) throw new Error("negative radius: " + r);
          if (this._x1 === null) {
            this._ += "M" + x0 + "," + y0;
          } else if (Math.abs(this._x1 - x0) > epsilon || Math.abs(this._y1 - y0) > epsilon) {
            this._ += "L" + x0 + "," + y0;
          }
          if (!r) return;
          if (da < 0) da = da % tau + tau;
          if (da > tauEpsilon) {
            this._ += "A" + r + "," + r + ",0,1," + cw + "," + (x - dx) + "," + (y - dy) + "A" + r + "," + r + ",0,1," + cw + "," + (this._x1 = x0) + "," + (this._y1 = y0);
          } else if (da > epsilon) {
            this._ += "A" + r + "," + r + ",0," + +(da >= pi) + "," + cw + "," + (this._x1 = x + r * Math.cos(a1)) + "," + (this._y1 = y + r * Math.sin(a1));
          }
        },
        rect: function(x, y, w, h) {
          this._ += "M" + (this._x0 = this._x1 = +x) + "," + (this._y0 = this._y1 = +y) + "h" + +w + "v" + +h + "h" + -w + "Z";
        },
        toString: function() {
          return this._;
        }
      };
      exports2.path = path;
      Object.defineProperty(exports2, "__esModule", { value: true });
    });
  }
});

// node_modules/d3-sankey/node_modules/d3-shape/dist/d3-shape.js
var require_d3_shape = __commonJS({
  "node_modules/d3-sankey/node_modules/d3-shape/dist/d3-shape.js"(exports, module) {
    init_polyfillShim();
    (function(global, factory) {
      typeof exports === "object" && typeof module !== "undefined" ? factory(exports, require_d3_path()) : typeof define === "function" && define.amd ? define(["exports", "d3-path"], factory) : (global = global || self, factory(global.d3 = global.d3 || {}, global.d3));
    })(exports, function(exports2, d3Path) {
      function constant(x2) {
        return function constant2() {
          return x2;
        };
      }
      var abs = Math.abs;
      var atan2 = Math.atan2;
      var cos = Math.cos;
      var max = Math.max;
      var min = Math.min;
      var sin = Math.sin;
      var sqrt = Math.sqrt;
      var epsilon = 1e-12;
      var pi = Math.PI;
      var halfPi = pi / 2;
      var tau = 2 * pi;
      function acos(x2) {
        return x2 > 1 ? 0 : x2 < -1 ? pi : Math.acos(x2);
      }
      function asin(x2) {
        return x2 >= 1 ? halfPi : x2 <= -1 ? -halfPi : Math.asin(x2);
      }
      function arcInnerRadius(d) {
        return d.innerRadius;
      }
      function arcOuterRadius(d) {
        return d.outerRadius;
      }
      function arcStartAngle(d) {
        return d.startAngle;
      }
      function arcEndAngle(d) {
        return d.endAngle;
      }
      function arcPadAngle(d) {
        return d && d.padAngle;
      }
      function intersect(x0, y0, x1, y1, x2, y2, x3, y3) {
        var x10 = x1 - x0, y10 = y1 - y0, x32 = x3 - x2, y32 = y3 - y2, t = y32 * x10 - x32 * y10;
        if (t * t < epsilon) return;
        t = (x32 * (y0 - y2) - y32 * (x0 - x2)) / t;
        return [x0 + t * x10, y0 + t * y10];
      }
      function cornerTangents(x0, y0, x1, y1, r1, rc, cw) {
        var x01 = x0 - x1, y01 = y0 - y1, lo = (cw ? rc : -rc) / sqrt(x01 * x01 + y01 * y01), ox = lo * y01, oy = -lo * x01, x11 = x0 + ox, y11 = y0 + oy, x10 = x1 + ox, y10 = y1 + oy, x00 = (x11 + x10) / 2, y00 = (y11 + y10) / 2, dx = x10 - x11, dy = y10 - y11, d2 = dx * dx + dy * dy, r = r1 - rc, D = x11 * y10 - x10 * y11, d = (dy < 0 ? -1 : 1) * sqrt(max(0, r * r * d2 - D * D)), cx0 = (D * dy - dx * d) / d2, cy0 = (-D * dx - dy * d) / d2, cx1 = (D * dy + dx * d) / d2, cy1 = (-D * dx + dy * d) / d2, dx0 = cx0 - x00, dy0 = cy0 - y00, dx1 = cx1 - x00, dy1 = cy1 - y00;
        if (dx0 * dx0 + dy0 * dy0 > dx1 * dx1 + dy1 * dy1) cx0 = cx1, cy0 = cy1;
        return {
          cx: cx0,
          cy: cy0,
          x01: -ox,
          y01: -oy,
          x11: cx0 * (r1 / r - 1),
          y11: cy0 * (r1 / r - 1)
        };
      }
      function arc() {
        var innerRadius = arcInnerRadius, outerRadius = arcOuterRadius, cornerRadius = constant(0), padRadius = null, startAngle = arcStartAngle, endAngle = arcEndAngle, padAngle = arcPadAngle, context = null;
        function arc2() {
          var buffer, r, r0 = +innerRadius.apply(this, arguments), r1 = +outerRadius.apply(this, arguments), a0 = startAngle.apply(this, arguments) - halfPi, a1 = endAngle.apply(this, arguments) - halfPi, da = abs(a1 - a0), cw = a1 > a0;
          if (!context) context = buffer = d3Path.path();
          if (r1 < r0) r = r1, r1 = r0, r0 = r;
          if (!(r1 > epsilon)) context.moveTo(0, 0);
          else if (da > tau - epsilon) {
            context.moveTo(r1 * cos(a0), r1 * sin(a0));
            context.arc(0, 0, r1, a0, a1, !cw);
            if (r0 > epsilon) {
              context.moveTo(r0 * cos(a1), r0 * sin(a1));
              context.arc(0, 0, r0, a1, a0, cw);
            }
          } else {
            var a01 = a0, a11 = a1, a00 = a0, a10 = a1, da0 = da, da1 = da, ap = padAngle.apply(this, arguments) / 2, rp = ap > epsilon && (padRadius ? +padRadius.apply(this, arguments) : sqrt(r0 * r0 + r1 * r1)), rc = min(abs(r1 - r0) / 2, +cornerRadius.apply(this, arguments)), rc0 = rc, rc1 = rc, t0, t1;
            if (rp > epsilon) {
              var p0 = asin(rp / r0 * sin(ap)), p1 = asin(rp / r1 * sin(ap));
              if ((da0 -= p0 * 2) > epsilon) p0 *= cw ? 1 : -1, a00 += p0, a10 -= p0;
              else da0 = 0, a00 = a10 = (a0 + a1) / 2;
              if ((da1 -= p1 * 2) > epsilon) p1 *= cw ? 1 : -1, a01 += p1, a11 -= p1;
              else da1 = 0, a01 = a11 = (a0 + a1) / 2;
            }
            var x01 = r1 * cos(a01), y01 = r1 * sin(a01), x10 = r0 * cos(a10), y10 = r0 * sin(a10);
            if (rc > epsilon) {
              var x11 = r1 * cos(a11), y11 = r1 * sin(a11), x00 = r0 * cos(a00), y00 = r0 * sin(a00), oc;
              if (da < pi && (oc = intersect(x01, y01, x00, y00, x11, y11, x10, y10))) {
                var ax = x01 - oc[0], ay = y01 - oc[1], bx = x11 - oc[0], by = y11 - oc[1], kc = 1 / sin(acos((ax * bx + ay * by) / (sqrt(ax * ax + ay * ay) * sqrt(bx * bx + by * by))) / 2), lc = sqrt(oc[0] * oc[0] + oc[1] * oc[1]);
                rc0 = min(rc, (r0 - lc) / (kc - 1));
                rc1 = min(rc, (r1 - lc) / (kc + 1));
              }
            }
            if (!(da1 > epsilon)) context.moveTo(x01, y01);
            else if (rc1 > epsilon) {
              t0 = cornerTangents(x00, y00, x01, y01, r1, rc1, cw);
              t1 = cornerTangents(x11, y11, x10, y10, r1, rc1, cw);
              context.moveTo(t0.cx + t0.x01, t0.cy + t0.y01);
              if (rc1 < rc) context.arc(t0.cx, t0.cy, rc1, atan2(t0.y01, t0.x01), atan2(t1.y01, t1.x01), !cw);
              else {
                context.arc(t0.cx, t0.cy, rc1, atan2(t0.y01, t0.x01), atan2(t0.y11, t0.x11), !cw);
                context.arc(0, 0, r1, atan2(t0.cy + t0.y11, t0.cx + t0.x11), atan2(t1.cy + t1.y11, t1.cx + t1.x11), !cw);
                context.arc(t1.cx, t1.cy, rc1, atan2(t1.y11, t1.x11), atan2(t1.y01, t1.x01), !cw);
              }
            } else context.moveTo(x01, y01), context.arc(0, 0, r1, a01, a11, !cw);
            if (!(r0 > epsilon) || !(da0 > epsilon)) context.lineTo(x10, y10);
            else if (rc0 > epsilon) {
              t0 = cornerTangents(x10, y10, x11, y11, r0, -rc0, cw);
              t1 = cornerTangents(x01, y01, x00, y00, r0, -rc0, cw);
              context.lineTo(t0.cx + t0.x01, t0.cy + t0.y01);
              if (rc0 < rc) context.arc(t0.cx, t0.cy, rc0, atan2(t0.y01, t0.x01), atan2(t1.y01, t1.x01), !cw);
              else {
                context.arc(t0.cx, t0.cy, rc0, atan2(t0.y01, t0.x01), atan2(t0.y11, t0.x11), !cw);
                context.arc(0, 0, r0, atan2(t0.cy + t0.y11, t0.cx + t0.x11), atan2(t1.cy + t1.y11, t1.cx + t1.x11), cw);
                context.arc(t1.cx, t1.cy, rc0, atan2(t1.y11, t1.x11), atan2(t1.y01, t1.x01), !cw);
              }
            } else context.arc(0, 0, r0, a10, a00, cw);
          }
          context.closePath();
          if (buffer) return context = null, buffer + "" || null;
        }
        arc2.centroid = function() {
          var r = (+innerRadius.apply(this, arguments) + +outerRadius.apply(this, arguments)) / 2, a2 = (+startAngle.apply(this, arguments) + +endAngle.apply(this, arguments)) / 2 - pi / 2;
          return [cos(a2) * r, sin(a2) * r];
        };
        arc2.innerRadius = function(_) {
          return arguments.length ? (innerRadius = typeof _ === "function" ? _ : constant(+_), arc2) : innerRadius;
        };
        arc2.outerRadius = function(_) {
          return arguments.length ? (outerRadius = typeof _ === "function" ? _ : constant(+_), arc2) : outerRadius;
        };
        arc2.cornerRadius = function(_) {
          return arguments.length ? (cornerRadius = typeof _ === "function" ? _ : constant(+_), arc2) : cornerRadius;
        };
        arc2.padRadius = function(_) {
          return arguments.length ? (padRadius = _ == null ? null : typeof _ === "function" ? _ : constant(+_), arc2) : padRadius;
        };
        arc2.startAngle = function(_) {
          return arguments.length ? (startAngle = typeof _ === "function" ? _ : constant(+_), arc2) : startAngle;
        };
        arc2.endAngle = function(_) {
          return arguments.length ? (endAngle = typeof _ === "function" ? _ : constant(+_), arc2) : endAngle;
        };
        arc2.padAngle = function(_) {
          return arguments.length ? (padAngle = typeof _ === "function" ? _ : constant(+_), arc2) : padAngle;
        };
        arc2.context = function(_) {
          return arguments.length ? (context = _ == null ? null : _, arc2) : context;
        };
        return arc2;
      }
      function Linear(context) {
        this._context = context;
      }
      Linear.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._point = 0;
        },
        lineEnd: function() {
          if (this._line || this._line !== 0 && this._point === 1) this._context.closePath();
          this._line = 1 - this._line;
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          switch (this._point) {
            case 0:
              this._point = 1;
              this._line ? this._context.lineTo(x2, y2) : this._context.moveTo(x2, y2);
              break;
            case 1:
              this._point = 2;
            // proceed
            default:
              this._context.lineTo(x2, y2);
              break;
          }
        }
      };
      function curveLinear(context) {
        return new Linear(context);
      }
      function x(p) {
        return p[0];
      }
      function y(p) {
        return p[1];
      }
      function line() {
        var x$1 = x, y$1 = y, defined = constant(true), context = null, curve = curveLinear, output = null;
        function line2(data) {
          var i, n = data.length, d, defined0 = false, buffer;
          if (context == null) output = curve(buffer = d3Path.path());
          for (i = 0; i <= n; ++i) {
            if (!(i < n && defined(d = data[i], i, data)) === defined0) {
              if (defined0 = !defined0) output.lineStart();
              else output.lineEnd();
            }
            if (defined0) output.point(+x$1(d, i, data), +y$1(d, i, data));
          }
          if (buffer) return output = null, buffer + "" || null;
        }
        line2.x = function(_) {
          return arguments.length ? (x$1 = typeof _ === "function" ? _ : constant(+_), line2) : x$1;
        };
        line2.y = function(_) {
          return arguments.length ? (y$1 = typeof _ === "function" ? _ : constant(+_), line2) : y$1;
        };
        line2.defined = function(_) {
          return arguments.length ? (defined = typeof _ === "function" ? _ : constant(!!_), line2) : defined;
        };
        line2.curve = function(_) {
          return arguments.length ? (curve = _, context != null && (output = curve(context)), line2) : curve;
        };
        line2.context = function(_) {
          return arguments.length ? (_ == null ? context = output = null : output = curve(context = _), line2) : context;
        };
        return line2;
      }
      function area() {
        var x0 = x, x1 = null, y0 = constant(0), y1 = y, defined = constant(true), context = null, curve = curveLinear, output = null;
        function area2(data) {
          var i, j, k2, n = data.length, d, defined0 = false, buffer, x0z = new Array(n), y0z = new Array(n);
          if (context == null) output = curve(buffer = d3Path.path());
          for (i = 0; i <= n; ++i) {
            if (!(i < n && defined(d = data[i], i, data)) === defined0) {
              if (defined0 = !defined0) {
                j = i;
                output.areaStart();
                output.lineStart();
              } else {
                output.lineEnd();
                output.lineStart();
                for (k2 = i - 1; k2 >= j; --k2) {
                  output.point(x0z[k2], y0z[k2]);
                }
                output.lineEnd();
                output.areaEnd();
              }
            }
            if (defined0) {
              x0z[i] = +x0(d, i, data), y0z[i] = +y0(d, i, data);
              output.point(x1 ? +x1(d, i, data) : x0z[i], y1 ? +y1(d, i, data) : y0z[i]);
            }
          }
          if (buffer) return output = null, buffer + "" || null;
        }
        function arealine() {
          return line().defined(defined).curve(curve).context(context);
        }
        area2.x = function(_) {
          return arguments.length ? (x0 = typeof _ === "function" ? _ : constant(+_), x1 = null, area2) : x0;
        };
        area2.x0 = function(_) {
          return arguments.length ? (x0 = typeof _ === "function" ? _ : constant(+_), area2) : x0;
        };
        area2.x1 = function(_) {
          return arguments.length ? (x1 = _ == null ? null : typeof _ === "function" ? _ : constant(+_), area2) : x1;
        };
        area2.y = function(_) {
          return arguments.length ? (y0 = typeof _ === "function" ? _ : constant(+_), y1 = null, area2) : y0;
        };
        area2.y0 = function(_) {
          return arguments.length ? (y0 = typeof _ === "function" ? _ : constant(+_), area2) : y0;
        };
        area2.y1 = function(_) {
          return arguments.length ? (y1 = _ == null ? null : typeof _ === "function" ? _ : constant(+_), area2) : y1;
        };
        area2.lineX0 = area2.lineY0 = function() {
          return arealine().x(x0).y(y0);
        };
        area2.lineY1 = function() {
          return arealine().x(x0).y(y1);
        };
        area2.lineX1 = function() {
          return arealine().x(x1).y(y0);
        };
        area2.defined = function(_) {
          return arguments.length ? (defined = typeof _ === "function" ? _ : constant(!!_), area2) : defined;
        };
        area2.curve = function(_) {
          return arguments.length ? (curve = _, context != null && (output = curve(context)), area2) : curve;
        };
        area2.context = function(_) {
          return arguments.length ? (_ == null ? context = output = null : output = curve(context = _), area2) : context;
        };
        return area2;
      }
      function descending(a2, b) {
        return b < a2 ? -1 : b > a2 ? 1 : b >= a2 ? 0 : NaN;
      }
      function identity(d) {
        return d;
      }
      function pie() {
        var value = identity, sortValues = descending, sort = null, startAngle = constant(0), endAngle = constant(tau), padAngle = constant(0);
        function pie2(data) {
          var i, n = data.length, j, k2, sum2 = 0, index = new Array(n), arcs = new Array(n), a0 = +startAngle.apply(this, arguments), da = Math.min(tau, Math.max(-tau, endAngle.apply(this, arguments) - a0)), a1, p = Math.min(Math.abs(da) / n, padAngle.apply(this, arguments)), pa = p * (da < 0 ? -1 : 1), v;
          for (i = 0; i < n; ++i) {
            if ((v = arcs[index[i] = i] = +value(data[i], i, data)) > 0) {
              sum2 += v;
            }
          }
          if (sortValues != null) index.sort(function(i2, j2) {
            return sortValues(arcs[i2], arcs[j2]);
          });
          else if (sort != null) index.sort(function(i2, j2) {
            return sort(data[i2], data[j2]);
          });
          for (i = 0, k2 = sum2 ? (da - n * pa) / sum2 : 0; i < n; ++i, a0 = a1) {
            j = index[i], v = arcs[j], a1 = a0 + (v > 0 ? v * k2 : 0) + pa, arcs[j] = {
              data: data[j],
              index: i,
              value: v,
              startAngle: a0,
              endAngle: a1,
              padAngle: p
            };
          }
          return arcs;
        }
        pie2.value = function(_) {
          return arguments.length ? (value = typeof _ === "function" ? _ : constant(+_), pie2) : value;
        };
        pie2.sortValues = function(_) {
          return arguments.length ? (sortValues = _, sort = null, pie2) : sortValues;
        };
        pie2.sort = function(_) {
          return arguments.length ? (sort = _, sortValues = null, pie2) : sort;
        };
        pie2.startAngle = function(_) {
          return arguments.length ? (startAngle = typeof _ === "function" ? _ : constant(+_), pie2) : startAngle;
        };
        pie2.endAngle = function(_) {
          return arguments.length ? (endAngle = typeof _ === "function" ? _ : constant(+_), pie2) : endAngle;
        };
        pie2.padAngle = function(_) {
          return arguments.length ? (padAngle = typeof _ === "function" ? _ : constant(+_), pie2) : padAngle;
        };
        return pie2;
      }
      var curveRadialLinear = curveRadial(curveLinear);
      function Radial(curve) {
        this._curve = curve;
      }
      Radial.prototype = {
        areaStart: function() {
          this._curve.areaStart();
        },
        areaEnd: function() {
          this._curve.areaEnd();
        },
        lineStart: function() {
          this._curve.lineStart();
        },
        lineEnd: function() {
          this._curve.lineEnd();
        },
        point: function(a2, r) {
          this._curve.point(r * Math.sin(a2), r * -Math.cos(a2));
        }
      };
      function curveRadial(curve) {
        function radial(context) {
          return new Radial(curve(context));
        }
        radial._curve = curve;
        return radial;
      }
      function lineRadial(l) {
        var c2 = l.curve;
        l.angle = l.x, delete l.x;
        l.radius = l.y, delete l.y;
        l.curve = function(_) {
          return arguments.length ? c2(curveRadial(_)) : c2()._curve;
        };
        return l;
      }
      function lineRadial$1() {
        return lineRadial(line().curve(curveRadialLinear));
      }
      function areaRadial() {
        var a2 = area().curve(curveRadialLinear), c2 = a2.curve, x0 = a2.lineX0, x1 = a2.lineX1, y0 = a2.lineY0, y1 = a2.lineY1;
        a2.angle = a2.x, delete a2.x;
        a2.startAngle = a2.x0, delete a2.x0;
        a2.endAngle = a2.x1, delete a2.x1;
        a2.radius = a2.y, delete a2.y;
        a2.innerRadius = a2.y0, delete a2.y0;
        a2.outerRadius = a2.y1, delete a2.y1;
        a2.lineStartAngle = function() {
          return lineRadial(x0());
        }, delete a2.lineX0;
        a2.lineEndAngle = function() {
          return lineRadial(x1());
        }, delete a2.lineX1;
        a2.lineInnerRadius = function() {
          return lineRadial(y0());
        }, delete a2.lineY0;
        a2.lineOuterRadius = function() {
          return lineRadial(y1());
        }, delete a2.lineY1;
        a2.curve = function(_) {
          return arguments.length ? c2(curveRadial(_)) : c2()._curve;
        };
        return a2;
      }
      function pointRadial(x2, y2) {
        return [(y2 = +y2) * Math.cos(x2 -= Math.PI / 2), y2 * Math.sin(x2)];
      }
      var slice = Array.prototype.slice;
      function linkSource(d) {
        return d.source;
      }
      function linkTarget(d) {
        return d.target;
      }
      function link(curve) {
        var source = linkSource, target = linkTarget, x$1 = x, y$1 = y, context = null;
        function link2() {
          var buffer, argv = slice.call(arguments), s2 = source.apply(this, argv), t = target.apply(this, argv);
          if (!context) context = buffer = d3Path.path();
          curve(context, +x$1.apply(this, (argv[0] = s2, argv)), +y$1.apply(this, argv), +x$1.apply(this, (argv[0] = t, argv)), +y$1.apply(this, argv));
          if (buffer) return context = null, buffer + "" || null;
        }
        link2.source = function(_) {
          return arguments.length ? (source = _, link2) : source;
        };
        link2.target = function(_) {
          return arguments.length ? (target = _, link2) : target;
        };
        link2.x = function(_) {
          return arguments.length ? (x$1 = typeof _ === "function" ? _ : constant(+_), link2) : x$1;
        };
        link2.y = function(_) {
          return arguments.length ? (y$1 = typeof _ === "function" ? _ : constant(+_), link2) : y$1;
        };
        link2.context = function(_) {
          return arguments.length ? (context = _ == null ? null : _, link2) : context;
        };
        return link2;
      }
      function curveHorizontal(context, x0, y0, x1, y1) {
        context.moveTo(x0, y0);
        context.bezierCurveTo(x0 = (x0 + x1) / 2, y0, x0, y1, x1, y1);
      }
      function curveVertical(context, x0, y0, x1, y1) {
        context.moveTo(x0, y0);
        context.bezierCurveTo(x0, y0 = (y0 + y1) / 2, x1, y0, x1, y1);
      }
      function curveRadial$1(context, x0, y0, x1, y1) {
        var p0 = pointRadial(x0, y0), p1 = pointRadial(x0, y0 = (y0 + y1) / 2), p2 = pointRadial(x1, y0), p3 = pointRadial(x1, y1);
        context.moveTo(p0[0], p0[1]);
        context.bezierCurveTo(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
      }
      function linkHorizontal() {
        return link(curveHorizontal);
      }
      function linkVertical() {
        return link(curveVertical);
      }
      function linkRadial() {
        var l = link(curveRadial$1);
        l.angle = l.x, delete l.x;
        l.radius = l.y, delete l.y;
        return l;
      }
      var circle = {
        draw: function(context, size) {
          var r = Math.sqrt(size / pi);
          context.moveTo(r, 0);
          context.arc(0, 0, r, 0, tau);
        }
      };
      var cross = {
        draw: function(context, size) {
          var r = Math.sqrt(size / 5) / 2;
          context.moveTo(-3 * r, -r);
          context.lineTo(-r, -r);
          context.lineTo(-r, -3 * r);
          context.lineTo(r, -3 * r);
          context.lineTo(r, -r);
          context.lineTo(3 * r, -r);
          context.lineTo(3 * r, r);
          context.lineTo(r, r);
          context.lineTo(r, 3 * r);
          context.lineTo(-r, 3 * r);
          context.lineTo(-r, r);
          context.lineTo(-3 * r, r);
          context.closePath();
        }
      };
      var tan30 = Math.sqrt(1 / 3), tan30_2 = tan30 * 2;
      var diamond = {
        draw: function(context, size) {
          var y2 = Math.sqrt(size / tan30_2), x2 = y2 * tan30;
          context.moveTo(0, -y2);
          context.lineTo(x2, 0);
          context.lineTo(0, y2);
          context.lineTo(-x2, 0);
          context.closePath();
        }
      };
      var ka = 0.8908130915292852, kr = Math.sin(pi / 10) / Math.sin(7 * pi / 10), kx = Math.sin(tau / 10) * kr, ky = -Math.cos(tau / 10) * kr;
      var star = {
        draw: function(context, size) {
          var r = Math.sqrt(size * ka), x2 = kx * r, y2 = ky * r;
          context.moveTo(0, -r);
          context.lineTo(x2, y2);
          for (var i = 1; i < 5; ++i) {
            var a2 = tau * i / 5, c2 = Math.cos(a2), s2 = Math.sin(a2);
            context.lineTo(s2 * r, -c2 * r);
            context.lineTo(c2 * x2 - s2 * y2, s2 * x2 + c2 * y2);
          }
          context.closePath();
        }
      };
      var square = {
        draw: function(context, size) {
          var w = Math.sqrt(size), x2 = -w / 2;
          context.rect(x2, x2, w, w);
        }
      };
      var sqrt3 = Math.sqrt(3);
      var triangle = {
        draw: function(context, size) {
          var y2 = -Math.sqrt(size / (sqrt3 * 3));
          context.moveTo(0, y2 * 2);
          context.lineTo(-sqrt3 * y2, -y2);
          context.lineTo(sqrt3 * y2, -y2);
          context.closePath();
        }
      };
      var c = -0.5, s = Math.sqrt(3) / 2, k = 1 / Math.sqrt(12), a = (k / 2 + 1) * 3;
      var wye = {
        draw: function(context, size) {
          var r = Math.sqrt(size / a), x0 = r / 2, y0 = r * k, x1 = x0, y1 = r * k + r, x2 = -x1, y2 = y1;
          context.moveTo(x0, y0);
          context.lineTo(x1, y1);
          context.lineTo(x2, y2);
          context.lineTo(c * x0 - s * y0, s * x0 + c * y0);
          context.lineTo(c * x1 - s * y1, s * x1 + c * y1);
          context.lineTo(c * x2 - s * y2, s * x2 + c * y2);
          context.lineTo(c * x0 + s * y0, c * y0 - s * x0);
          context.lineTo(c * x1 + s * y1, c * y1 - s * x1);
          context.lineTo(c * x2 + s * y2, c * y2 - s * x2);
          context.closePath();
        }
      };
      var symbols = [
        circle,
        cross,
        diamond,
        square,
        star,
        triangle,
        wye
      ];
      function symbol() {
        var type = constant(circle), size = constant(64), context = null;
        function symbol2() {
          var buffer;
          if (!context) context = buffer = d3Path.path();
          type.apply(this, arguments).draw(context, +size.apply(this, arguments));
          if (buffer) return context = null, buffer + "" || null;
        }
        symbol2.type = function(_) {
          return arguments.length ? (type = typeof _ === "function" ? _ : constant(_), symbol2) : type;
        };
        symbol2.size = function(_) {
          return arguments.length ? (size = typeof _ === "function" ? _ : constant(+_), symbol2) : size;
        };
        symbol2.context = function(_) {
          return arguments.length ? (context = _ == null ? null : _, symbol2) : context;
        };
        return symbol2;
      }
      function noop() {
      }
      function point(that, x2, y2) {
        that._context.bezierCurveTo(
          (2 * that._x0 + that._x1) / 3,
          (2 * that._y0 + that._y1) / 3,
          (that._x0 + 2 * that._x1) / 3,
          (that._y0 + 2 * that._y1) / 3,
          (that._x0 + 4 * that._x1 + x2) / 6,
          (that._y0 + 4 * that._y1 + y2) / 6
        );
      }
      function Basis(context) {
        this._context = context;
      }
      Basis.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._x0 = this._x1 = this._y0 = this._y1 = NaN;
          this._point = 0;
        },
        lineEnd: function() {
          switch (this._point) {
            case 3:
              point(this, this._x1, this._y1);
            // proceed
            case 2:
              this._context.lineTo(this._x1, this._y1);
              break;
          }
          if (this._line || this._line !== 0 && this._point === 1) this._context.closePath();
          this._line = 1 - this._line;
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          switch (this._point) {
            case 0:
              this._point = 1;
              this._line ? this._context.lineTo(x2, y2) : this._context.moveTo(x2, y2);
              break;
            case 1:
              this._point = 2;
              break;
            case 2:
              this._point = 3;
              this._context.lineTo((5 * this._x0 + this._x1) / 6, (5 * this._y0 + this._y1) / 6);
            // proceed
            default:
              point(this, x2, y2);
              break;
          }
          this._x0 = this._x1, this._x1 = x2;
          this._y0 = this._y1, this._y1 = y2;
        }
      };
      function basis(context) {
        return new Basis(context);
      }
      function BasisClosed(context) {
        this._context = context;
      }
      BasisClosed.prototype = {
        areaStart: noop,
        areaEnd: noop,
        lineStart: function() {
          this._x0 = this._x1 = this._x2 = this._x3 = this._x4 = this._y0 = this._y1 = this._y2 = this._y3 = this._y4 = NaN;
          this._point = 0;
        },
        lineEnd: function() {
          switch (this._point) {
            case 1: {
              this._context.moveTo(this._x2, this._y2);
              this._context.closePath();
              break;
            }
            case 2: {
              this._context.moveTo((this._x2 + 2 * this._x3) / 3, (this._y2 + 2 * this._y3) / 3);
              this._context.lineTo((this._x3 + 2 * this._x2) / 3, (this._y3 + 2 * this._y2) / 3);
              this._context.closePath();
              break;
            }
            case 3: {
              this.point(this._x2, this._y2);
              this.point(this._x3, this._y3);
              this.point(this._x4, this._y4);
              break;
            }
          }
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          switch (this._point) {
            case 0:
              this._point = 1;
              this._x2 = x2, this._y2 = y2;
              break;
            case 1:
              this._point = 2;
              this._x3 = x2, this._y3 = y2;
              break;
            case 2:
              this._point = 3;
              this._x4 = x2, this._y4 = y2;
              this._context.moveTo((this._x0 + 4 * this._x1 + x2) / 6, (this._y0 + 4 * this._y1 + y2) / 6);
              break;
            default:
              point(this, x2, y2);
              break;
          }
          this._x0 = this._x1, this._x1 = x2;
          this._y0 = this._y1, this._y1 = y2;
        }
      };
      function basisClosed(context) {
        return new BasisClosed(context);
      }
      function BasisOpen(context) {
        this._context = context;
      }
      BasisOpen.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._x0 = this._x1 = this._y0 = this._y1 = NaN;
          this._point = 0;
        },
        lineEnd: function() {
          if (this._line || this._line !== 0 && this._point === 3) this._context.closePath();
          this._line = 1 - this._line;
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          switch (this._point) {
            case 0:
              this._point = 1;
              break;
            case 1:
              this._point = 2;
              break;
            case 2:
              this._point = 3;
              var x0 = (this._x0 + 4 * this._x1 + x2) / 6, y0 = (this._y0 + 4 * this._y1 + y2) / 6;
              this._line ? this._context.lineTo(x0, y0) : this._context.moveTo(x0, y0);
              break;
            case 3:
              this._point = 4;
            // proceed
            default:
              point(this, x2, y2);
              break;
          }
          this._x0 = this._x1, this._x1 = x2;
          this._y0 = this._y1, this._y1 = y2;
        }
      };
      function basisOpen(context) {
        return new BasisOpen(context);
      }
      function Bundle(context, beta) {
        this._basis = new Basis(context);
        this._beta = beta;
      }
      Bundle.prototype = {
        lineStart: function() {
          this._x = [];
          this._y = [];
          this._basis.lineStart();
        },
        lineEnd: function() {
          var x2 = this._x, y2 = this._y, j = x2.length - 1;
          if (j > 0) {
            var x0 = x2[0], y0 = y2[0], dx = x2[j] - x0, dy = y2[j] - y0, i = -1, t;
            while (++i <= j) {
              t = i / j;
              this._basis.point(
                this._beta * x2[i] + (1 - this._beta) * (x0 + t * dx),
                this._beta * y2[i] + (1 - this._beta) * (y0 + t * dy)
              );
            }
          }
          this._x = this._y = null;
          this._basis.lineEnd();
        },
        point: function(x2, y2) {
          this._x.push(+x2);
          this._y.push(+y2);
        }
      };
      var bundle = function custom(beta) {
        function bundle2(context) {
          return beta === 1 ? new Basis(context) : new Bundle(context, beta);
        }
        bundle2.beta = function(beta2) {
          return custom(+beta2);
        };
        return bundle2;
      }(0.85);
      function point$1(that, x2, y2) {
        that._context.bezierCurveTo(
          that._x1 + that._k * (that._x2 - that._x0),
          that._y1 + that._k * (that._y2 - that._y0),
          that._x2 + that._k * (that._x1 - x2),
          that._y2 + that._k * (that._y1 - y2),
          that._x2,
          that._y2
        );
      }
      function Cardinal(context, tension) {
        this._context = context;
        this._k = (1 - tension) / 6;
      }
      Cardinal.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._x0 = this._x1 = this._x2 = this._y0 = this._y1 = this._y2 = NaN;
          this._point = 0;
        },
        lineEnd: function() {
          switch (this._point) {
            case 2:
              this._context.lineTo(this._x2, this._y2);
              break;
            case 3:
              point$1(this, this._x1, this._y1);
              break;
          }
          if (this._line || this._line !== 0 && this._point === 1) this._context.closePath();
          this._line = 1 - this._line;
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          switch (this._point) {
            case 0:
              this._point = 1;
              this._line ? this._context.lineTo(x2, y2) : this._context.moveTo(x2, y2);
              break;
            case 1:
              this._point = 2;
              this._x1 = x2, this._y1 = y2;
              break;
            case 2:
              this._point = 3;
            // proceed
            default:
              point$1(this, x2, y2);
              break;
          }
          this._x0 = this._x1, this._x1 = this._x2, this._x2 = x2;
          this._y0 = this._y1, this._y1 = this._y2, this._y2 = y2;
        }
      };
      var cardinal = function custom(tension) {
        function cardinal2(context) {
          return new Cardinal(context, tension);
        }
        cardinal2.tension = function(tension2) {
          return custom(+tension2);
        };
        return cardinal2;
      }(0);
      function CardinalClosed(context, tension) {
        this._context = context;
        this._k = (1 - tension) / 6;
      }
      CardinalClosed.prototype = {
        areaStart: noop,
        areaEnd: noop,
        lineStart: function() {
          this._x0 = this._x1 = this._x2 = this._x3 = this._x4 = this._x5 = this._y0 = this._y1 = this._y2 = this._y3 = this._y4 = this._y5 = NaN;
          this._point = 0;
        },
        lineEnd: function() {
          switch (this._point) {
            case 1: {
              this._context.moveTo(this._x3, this._y3);
              this._context.closePath();
              break;
            }
            case 2: {
              this._context.lineTo(this._x3, this._y3);
              this._context.closePath();
              break;
            }
            case 3: {
              this.point(this._x3, this._y3);
              this.point(this._x4, this._y4);
              this.point(this._x5, this._y5);
              break;
            }
          }
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          switch (this._point) {
            case 0:
              this._point = 1;
              this._x3 = x2, this._y3 = y2;
              break;
            case 1:
              this._point = 2;
              this._context.moveTo(this._x4 = x2, this._y4 = y2);
              break;
            case 2:
              this._point = 3;
              this._x5 = x2, this._y5 = y2;
              break;
            default:
              point$1(this, x2, y2);
              break;
          }
          this._x0 = this._x1, this._x1 = this._x2, this._x2 = x2;
          this._y0 = this._y1, this._y1 = this._y2, this._y2 = y2;
        }
      };
      var cardinalClosed = function custom(tension) {
        function cardinal2(context) {
          return new CardinalClosed(context, tension);
        }
        cardinal2.tension = function(tension2) {
          return custom(+tension2);
        };
        return cardinal2;
      }(0);
      function CardinalOpen(context, tension) {
        this._context = context;
        this._k = (1 - tension) / 6;
      }
      CardinalOpen.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._x0 = this._x1 = this._x2 = this._y0 = this._y1 = this._y2 = NaN;
          this._point = 0;
        },
        lineEnd: function() {
          if (this._line || this._line !== 0 && this._point === 3) this._context.closePath();
          this._line = 1 - this._line;
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          switch (this._point) {
            case 0:
              this._point = 1;
              break;
            case 1:
              this._point = 2;
              break;
            case 2:
              this._point = 3;
              this._line ? this._context.lineTo(this._x2, this._y2) : this._context.moveTo(this._x2, this._y2);
              break;
            case 3:
              this._point = 4;
            // proceed
            default:
              point$1(this, x2, y2);
              break;
          }
          this._x0 = this._x1, this._x1 = this._x2, this._x2 = x2;
          this._y0 = this._y1, this._y1 = this._y2, this._y2 = y2;
        }
      };
      var cardinalOpen = function custom(tension) {
        function cardinal2(context) {
          return new CardinalOpen(context, tension);
        }
        cardinal2.tension = function(tension2) {
          return custom(+tension2);
        };
        return cardinal2;
      }(0);
      function point$2(that, x2, y2) {
        var x1 = that._x1, y1 = that._y1, x22 = that._x2, y22 = that._y2;
        if (that._l01_a > epsilon) {
          var a2 = 2 * that._l01_2a + 3 * that._l01_a * that._l12_a + that._l12_2a, n = 3 * that._l01_a * (that._l01_a + that._l12_a);
          x1 = (x1 * a2 - that._x0 * that._l12_2a + that._x2 * that._l01_2a) / n;
          y1 = (y1 * a2 - that._y0 * that._l12_2a + that._y2 * that._l01_2a) / n;
        }
        if (that._l23_a > epsilon) {
          var b = 2 * that._l23_2a + 3 * that._l23_a * that._l12_a + that._l12_2a, m = 3 * that._l23_a * (that._l23_a + that._l12_a);
          x22 = (x22 * b + that._x1 * that._l23_2a - x2 * that._l12_2a) / m;
          y22 = (y22 * b + that._y1 * that._l23_2a - y2 * that._l12_2a) / m;
        }
        that._context.bezierCurveTo(x1, y1, x22, y22, that._x2, that._y2);
      }
      function CatmullRom(context, alpha) {
        this._context = context;
        this._alpha = alpha;
      }
      CatmullRom.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._x0 = this._x1 = this._x2 = this._y0 = this._y1 = this._y2 = NaN;
          this._l01_a = this._l12_a = this._l23_a = this._l01_2a = this._l12_2a = this._l23_2a = this._point = 0;
        },
        lineEnd: function() {
          switch (this._point) {
            case 2:
              this._context.lineTo(this._x2, this._y2);
              break;
            case 3:
              this.point(this._x2, this._y2);
              break;
          }
          if (this._line || this._line !== 0 && this._point === 1) this._context.closePath();
          this._line = 1 - this._line;
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          if (this._point) {
            var x23 = this._x2 - x2, y23 = this._y2 - y2;
            this._l23_a = Math.sqrt(this._l23_2a = Math.pow(x23 * x23 + y23 * y23, this._alpha));
          }
          switch (this._point) {
            case 0:
              this._point = 1;
              this._line ? this._context.lineTo(x2, y2) : this._context.moveTo(x2, y2);
              break;
            case 1:
              this._point = 2;
              break;
            case 2:
              this._point = 3;
            // proceed
            default:
              point$2(this, x2, y2);
              break;
          }
          this._l01_a = this._l12_a, this._l12_a = this._l23_a;
          this._l01_2a = this._l12_2a, this._l12_2a = this._l23_2a;
          this._x0 = this._x1, this._x1 = this._x2, this._x2 = x2;
          this._y0 = this._y1, this._y1 = this._y2, this._y2 = y2;
        }
      };
      var catmullRom = function custom(alpha) {
        function catmullRom2(context) {
          return alpha ? new CatmullRom(context, alpha) : new Cardinal(context, 0);
        }
        catmullRom2.alpha = function(alpha2) {
          return custom(+alpha2);
        };
        return catmullRom2;
      }(0.5);
      function CatmullRomClosed(context, alpha) {
        this._context = context;
        this._alpha = alpha;
      }
      CatmullRomClosed.prototype = {
        areaStart: noop,
        areaEnd: noop,
        lineStart: function() {
          this._x0 = this._x1 = this._x2 = this._x3 = this._x4 = this._x5 = this._y0 = this._y1 = this._y2 = this._y3 = this._y4 = this._y5 = NaN;
          this._l01_a = this._l12_a = this._l23_a = this._l01_2a = this._l12_2a = this._l23_2a = this._point = 0;
        },
        lineEnd: function() {
          switch (this._point) {
            case 1: {
              this._context.moveTo(this._x3, this._y3);
              this._context.closePath();
              break;
            }
            case 2: {
              this._context.lineTo(this._x3, this._y3);
              this._context.closePath();
              break;
            }
            case 3: {
              this.point(this._x3, this._y3);
              this.point(this._x4, this._y4);
              this.point(this._x5, this._y5);
              break;
            }
          }
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          if (this._point) {
            var x23 = this._x2 - x2, y23 = this._y2 - y2;
            this._l23_a = Math.sqrt(this._l23_2a = Math.pow(x23 * x23 + y23 * y23, this._alpha));
          }
          switch (this._point) {
            case 0:
              this._point = 1;
              this._x3 = x2, this._y3 = y2;
              break;
            case 1:
              this._point = 2;
              this._context.moveTo(this._x4 = x2, this._y4 = y2);
              break;
            case 2:
              this._point = 3;
              this._x5 = x2, this._y5 = y2;
              break;
            default:
              point$2(this, x2, y2);
              break;
          }
          this._l01_a = this._l12_a, this._l12_a = this._l23_a;
          this._l01_2a = this._l12_2a, this._l12_2a = this._l23_2a;
          this._x0 = this._x1, this._x1 = this._x2, this._x2 = x2;
          this._y0 = this._y1, this._y1 = this._y2, this._y2 = y2;
        }
      };
      var catmullRomClosed = function custom(alpha) {
        function catmullRom2(context) {
          return alpha ? new CatmullRomClosed(context, alpha) : new CardinalClosed(context, 0);
        }
        catmullRom2.alpha = function(alpha2) {
          return custom(+alpha2);
        };
        return catmullRom2;
      }(0.5);
      function CatmullRomOpen(context, alpha) {
        this._context = context;
        this._alpha = alpha;
      }
      CatmullRomOpen.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._x0 = this._x1 = this._x2 = this._y0 = this._y1 = this._y2 = NaN;
          this._l01_a = this._l12_a = this._l23_a = this._l01_2a = this._l12_2a = this._l23_2a = this._point = 0;
        },
        lineEnd: function() {
          if (this._line || this._line !== 0 && this._point === 3) this._context.closePath();
          this._line = 1 - this._line;
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          if (this._point) {
            var x23 = this._x2 - x2, y23 = this._y2 - y2;
            this._l23_a = Math.sqrt(this._l23_2a = Math.pow(x23 * x23 + y23 * y23, this._alpha));
          }
          switch (this._point) {
            case 0:
              this._point = 1;
              break;
            case 1:
              this._point = 2;
              break;
            case 2:
              this._point = 3;
              this._line ? this._context.lineTo(this._x2, this._y2) : this._context.moveTo(this._x2, this._y2);
              break;
            case 3:
              this._point = 4;
            // proceed
            default:
              point$2(this, x2, y2);
              break;
          }
          this._l01_a = this._l12_a, this._l12_a = this._l23_a;
          this._l01_2a = this._l12_2a, this._l12_2a = this._l23_2a;
          this._x0 = this._x1, this._x1 = this._x2, this._x2 = x2;
          this._y0 = this._y1, this._y1 = this._y2, this._y2 = y2;
        }
      };
      var catmullRomOpen = function custom(alpha) {
        function catmullRom2(context) {
          return alpha ? new CatmullRomOpen(context, alpha) : new CardinalOpen(context, 0);
        }
        catmullRom2.alpha = function(alpha2) {
          return custom(+alpha2);
        };
        return catmullRom2;
      }(0.5);
      function LinearClosed(context) {
        this._context = context;
      }
      LinearClosed.prototype = {
        areaStart: noop,
        areaEnd: noop,
        lineStart: function() {
          this._point = 0;
        },
        lineEnd: function() {
          if (this._point) this._context.closePath();
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          if (this._point) this._context.lineTo(x2, y2);
          else this._point = 1, this._context.moveTo(x2, y2);
        }
      };
      function linearClosed(context) {
        return new LinearClosed(context);
      }
      function sign(x2) {
        return x2 < 0 ? -1 : 1;
      }
      function slope3(that, x2, y2) {
        var h0 = that._x1 - that._x0, h1 = x2 - that._x1, s0 = (that._y1 - that._y0) / (h0 || h1 < 0 && -0), s1 = (y2 - that._y1) / (h1 || h0 < 0 && -0), p = (s0 * h1 + s1 * h0) / (h0 + h1);
        return (sign(s0) + sign(s1)) * Math.min(Math.abs(s0), Math.abs(s1), 0.5 * Math.abs(p)) || 0;
      }
      function slope2(that, t) {
        var h = that._x1 - that._x0;
        return h ? (3 * (that._y1 - that._y0) / h - t) / 2 : t;
      }
      function point$3(that, t0, t1) {
        var x0 = that._x0, y0 = that._y0, x1 = that._x1, y1 = that._y1, dx = (x1 - x0) / 3;
        that._context.bezierCurveTo(x0 + dx, y0 + dx * t0, x1 - dx, y1 - dx * t1, x1, y1);
      }
      function MonotoneX(context) {
        this._context = context;
      }
      MonotoneX.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._x0 = this._x1 = this._y0 = this._y1 = this._t0 = NaN;
          this._point = 0;
        },
        lineEnd: function() {
          switch (this._point) {
            case 2:
              this._context.lineTo(this._x1, this._y1);
              break;
            case 3:
              point$3(this, this._t0, slope2(this, this._t0));
              break;
          }
          if (this._line || this._line !== 0 && this._point === 1) this._context.closePath();
          this._line = 1 - this._line;
        },
        point: function(x2, y2) {
          var t1 = NaN;
          x2 = +x2, y2 = +y2;
          if (x2 === this._x1 && y2 === this._y1) return;
          switch (this._point) {
            case 0:
              this._point = 1;
              this._line ? this._context.lineTo(x2, y2) : this._context.moveTo(x2, y2);
              break;
            case 1:
              this._point = 2;
              break;
            case 2:
              this._point = 3;
              point$3(this, slope2(this, t1 = slope3(this, x2, y2)), t1);
              break;
            default:
              point$3(this, this._t0, t1 = slope3(this, x2, y2));
              break;
          }
          this._x0 = this._x1, this._x1 = x2;
          this._y0 = this._y1, this._y1 = y2;
          this._t0 = t1;
        }
      };
      function MonotoneY(context) {
        this._context = new ReflectContext(context);
      }
      (MonotoneY.prototype = Object.create(MonotoneX.prototype)).point = function(x2, y2) {
        MonotoneX.prototype.point.call(this, y2, x2);
      };
      function ReflectContext(context) {
        this._context = context;
      }
      ReflectContext.prototype = {
        moveTo: function(x2, y2) {
          this._context.moveTo(y2, x2);
        },
        closePath: function() {
          this._context.closePath();
        },
        lineTo: function(x2, y2) {
          this._context.lineTo(y2, x2);
        },
        bezierCurveTo: function(x1, y1, x2, y2, x3, y3) {
          this._context.bezierCurveTo(y1, x1, y2, x2, y3, x3);
        }
      };
      function monotoneX(context) {
        return new MonotoneX(context);
      }
      function monotoneY(context) {
        return new MonotoneY(context);
      }
      function Natural(context) {
        this._context = context;
      }
      Natural.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._x = [];
          this._y = [];
        },
        lineEnd: function() {
          var x2 = this._x, y2 = this._y, n = x2.length;
          if (n) {
            this._line ? this._context.lineTo(x2[0], y2[0]) : this._context.moveTo(x2[0], y2[0]);
            if (n === 2) {
              this._context.lineTo(x2[1], y2[1]);
            } else {
              var px = controlPoints(x2), py = controlPoints(y2);
              for (var i0 = 0, i1 = 1; i1 < n; ++i0, ++i1) {
                this._context.bezierCurveTo(px[0][i0], py[0][i0], px[1][i0], py[1][i0], x2[i1], y2[i1]);
              }
            }
          }
          if (this._line || this._line !== 0 && n === 1) this._context.closePath();
          this._line = 1 - this._line;
          this._x = this._y = null;
        },
        point: function(x2, y2) {
          this._x.push(+x2);
          this._y.push(+y2);
        }
      };
      function controlPoints(x2) {
        var i, n = x2.length - 1, m, a2 = new Array(n), b = new Array(n), r = new Array(n);
        a2[0] = 0, b[0] = 2, r[0] = x2[0] + 2 * x2[1];
        for (i = 1; i < n - 1; ++i) a2[i] = 1, b[i] = 4, r[i] = 4 * x2[i] + 2 * x2[i + 1];
        a2[n - 1] = 2, b[n - 1] = 7, r[n - 1] = 8 * x2[n - 1] + x2[n];
        for (i = 1; i < n; ++i) m = a2[i] / b[i - 1], b[i] -= m, r[i] -= m * r[i - 1];
        a2[n - 1] = r[n - 1] / b[n - 1];
        for (i = n - 2; i >= 0; --i) a2[i] = (r[i] - a2[i + 1]) / b[i];
        b[n - 1] = (x2[n] + a2[n - 1]) / 2;
        for (i = 0; i < n - 1; ++i) b[i] = 2 * x2[i + 1] - a2[i + 1];
        return [a2, b];
      }
      function natural(context) {
        return new Natural(context);
      }
      function Step(context, t) {
        this._context = context;
        this._t = t;
      }
      Step.prototype = {
        areaStart: function() {
          this._line = 0;
        },
        areaEnd: function() {
          this._line = NaN;
        },
        lineStart: function() {
          this._x = this._y = NaN;
          this._point = 0;
        },
        lineEnd: function() {
          if (0 < this._t && this._t < 1 && this._point === 2) this._context.lineTo(this._x, this._y);
          if (this._line || this._line !== 0 && this._point === 1) this._context.closePath();
          if (this._line >= 0) this._t = 1 - this._t, this._line = 1 - this._line;
        },
        point: function(x2, y2) {
          x2 = +x2, y2 = +y2;
          switch (this._point) {
            case 0:
              this._point = 1;
              this._line ? this._context.lineTo(x2, y2) : this._context.moveTo(x2, y2);
              break;
            case 1:
              this._point = 2;
            // proceed
            default: {
              if (this._t <= 0) {
                this._context.lineTo(this._x, y2);
                this._context.lineTo(x2, y2);
              } else {
                var x1 = this._x * (1 - this._t) + x2 * this._t;
                this._context.lineTo(x1, this._y);
                this._context.lineTo(x1, y2);
              }
              break;
            }
          }
          this._x = x2, this._y = y2;
        }
      };
      function step(context) {
        return new Step(context, 0.5);
      }
      function stepBefore(context) {
        return new Step(context, 0);
      }
      function stepAfter(context) {
        return new Step(context, 1);
      }
      function none(series, order) {
        if (!((n = series.length) > 1)) return;
        for (var i = 1, j, s0, s1 = series[order[0]], n, m = s1.length; i < n; ++i) {
          s0 = s1, s1 = series[order[i]];
          for (j = 0; j < m; ++j) {
            s1[j][1] += s1[j][0] = isNaN(s0[j][1]) ? s0[j][0] : s0[j][1];
          }
        }
      }
      function none$1(series) {
        var n = series.length, o = new Array(n);
        while (--n >= 0) o[n] = n;
        return o;
      }
      function stackValue(d, key) {
        return d[key];
      }
      function stack() {
        var keys = constant([]), order = none$1, offset = none, value = stackValue;
        function stack2(data) {
          var kz = keys.apply(this, arguments), i, m = data.length, n = kz.length, sz = new Array(n), oz;
          for (i = 0; i < n; ++i) {
            for (var ki = kz[i], si = sz[i] = new Array(m), j = 0, sij; j < m; ++j) {
              si[j] = sij = [0, +value(data[j], ki, j, data)];
              sij.data = data[j];
            }
            si.key = ki;
          }
          for (i = 0, oz = order(sz); i < n; ++i) {
            sz[oz[i]].index = i;
          }
          offset(sz, oz);
          return sz;
        }
        stack2.keys = function(_) {
          return arguments.length ? (keys = typeof _ === "function" ? _ : constant(slice.call(_)), stack2) : keys;
        };
        stack2.value = function(_) {
          return arguments.length ? (value = typeof _ === "function" ? _ : constant(+_), stack2) : value;
        };
        stack2.order = function(_) {
          return arguments.length ? (order = _ == null ? none$1 : typeof _ === "function" ? _ : constant(slice.call(_)), stack2) : order;
        };
        stack2.offset = function(_) {
          return arguments.length ? (offset = _ == null ? none : _, stack2) : offset;
        };
        return stack2;
      }
      function expand(series, order) {
        if (!((n = series.length) > 0)) return;
        for (var i, n, j = 0, m = series[0].length, y2; j < m; ++j) {
          for (y2 = i = 0; i < n; ++i) y2 += series[i][j][1] || 0;
          if (y2) for (i = 0; i < n; ++i) series[i][j][1] /= y2;
        }
        none(series, order);
      }
      function diverging(series, order) {
        if (!((n = series.length) > 0)) return;
        for (var i, j = 0, d, dy, yp, yn, n, m = series[order[0]].length; j < m; ++j) {
          for (yp = yn = 0, i = 0; i < n; ++i) {
            if ((dy = (d = series[order[i]][j])[1] - d[0]) > 0) {
              d[0] = yp, d[1] = yp += dy;
            } else if (dy < 0) {
              d[1] = yn, d[0] = yn += dy;
            } else {
              d[0] = 0, d[1] = dy;
            }
          }
        }
      }
      function silhouette(series, order) {
        if (!((n = series.length) > 0)) return;
        for (var j = 0, s0 = series[order[0]], n, m = s0.length; j < m; ++j) {
          for (var i = 0, y2 = 0; i < n; ++i) y2 += series[i][j][1] || 0;
          s0[j][1] += s0[j][0] = -y2 / 2;
        }
        none(series, order);
      }
      function wiggle(series, order) {
        if (!((n = series.length) > 0) || !((m = (s0 = series[order[0]]).length) > 0)) return;
        for (var y2 = 0, j = 1, s0, m, n; j < m; ++j) {
          for (var i = 0, s1 = 0, s2 = 0; i < n; ++i) {
            var si = series[order[i]], sij0 = si[j][1] || 0, sij1 = si[j - 1][1] || 0, s3 = (sij0 - sij1) / 2;
            for (var k2 = 0; k2 < i; ++k2) {
              var sk = series[order[k2]], skj0 = sk[j][1] || 0, skj1 = sk[j - 1][1] || 0;
              s3 += skj0 - skj1;
            }
            s1 += sij0, s2 += s3 * sij0;
          }
          s0[j - 1][1] += s0[j - 1][0] = y2;
          if (s1) y2 -= s2 / s1;
        }
        s0[j - 1][1] += s0[j - 1][0] = y2;
        none(series, order);
      }
      function appearance(series) {
        var peaks = series.map(peak);
        return none$1(series).sort(function(a2, b) {
          return peaks[a2] - peaks[b];
        });
      }
      function peak(series) {
        var i = -1, j = 0, n = series.length, vi, vj = -Infinity;
        while (++i < n) if ((vi = +series[i][1]) > vj) vj = vi, j = i;
        return j;
      }
      function ascending(series) {
        var sums = series.map(sum);
        return none$1(series).sort(function(a2, b) {
          return sums[a2] - sums[b];
        });
      }
      function sum(series) {
        var s2 = 0, i = -1, n = series.length, v;
        while (++i < n) if (v = +series[i][1]) s2 += v;
        return s2;
      }
      function descending$1(series) {
        return ascending(series).reverse();
      }
      function insideOut(series) {
        var n = series.length, i, j, sums = series.map(sum), order = appearance(series), top = 0, bottom = 0, tops = [], bottoms = [];
        for (i = 0; i < n; ++i) {
          j = order[i];
          if (top < bottom) {
            top += sums[j];
            tops.push(j);
          } else {
            bottom += sums[j];
            bottoms.push(j);
          }
        }
        return bottoms.reverse().concat(tops);
      }
      function reverse(series) {
        return none$1(series).reverse();
      }
      exports2.arc = arc;
      exports2.area = area;
      exports2.areaRadial = areaRadial;
      exports2.curveBasis = basis;
      exports2.curveBasisClosed = basisClosed;
      exports2.curveBasisOpen = basisOpen;
      exports2.curveBundle = bundle;
      exports2.curveCardinal = cardinal;
      exports2.curveCardinalClosed = cardinalClosed;
      exports2.curveCardinalOpen = cardinalOpen;
      exports2.curveCatmullRom = catmullRom;
      exports2.curveCatmullRomClosed = catmullRomClosed;
      exports2.curveCatmullRomOpen = catmullRomOpen;
      exports2.curveLinear = curveLinear;
      exports2.curveLinearClosed = linearClosed;
      exports2.curveMonotoneX = monotoneX;
      exports2.curveMonotoneY = monotoneY;
      exports2.curveNatural = natural;
      exports2.curveStep = step;
      exports2.curveStepAfter = stepAfter;
      exports2.curveStepBefore = stepBefore;
      exports2.line = line;
      exports2.lineRadial = lineRadial$1;
      exports2.linkHorizontal = linkHorizontal;
      exports2.linkRadial = linkRadial;
      exports2.linkVertical = linkVertical;
      exports2.pie = pie;
      exports2.pointRadial = pointRadial;
      exports2.radialArea = areaRadial;
      exports2.radialLine = lineRadial$1;
      exports2.stack = stack;
      exports2.stackOffsetDiverging = diverging;
      exports2.stackOffsetExpand = expand;
      exports2.stackOffsetNone = none;
      exports2.stackOffsetSilhouette = silhouette;
      exports2.stackOffsetWiggle = wiggle;
      exports2.stackOrderAppearance = appearance;
      exports2.stackOrderAscending = ascending;
      exports2.stackOrderDescending = descending$1;
      exports2.stackOrderInsideOut = insideOut;
      exports2.stackOrderNone = none$1;
      exports2.stackOrderReverse = reverse;
      exports2.symbol = symbol;
      exports2.symbolCircle = circle;
      exports2.symbolCross = cross;
      exports2.symbolDiamond = diamond;
      exports2.symbolSquare = square;
      exports2.symbolStar = star;
      exports2.symbolTriangle = triangle;
      exports2.symbolWye = wye;
      exports2.symbols = symbols;
      Object.defineProperty(exports2, "__esModule", { value: true });
    });
  }
});

// node_modules/d3-sankey/dist/d3-sankey.js
var require_d3_sankey = __commonJS({
  "node_modules/d3-sankey/dist/d3-sankey.js"(exports, module) {
    init_polyfillShim();
    (function(global, factory) {
      typeof exports === "object" && typeof module !== "undefined" ? factory(exports, require_d3_array(), require_d3_shape()) : typeof define === "function" && define.amd ? define(["exports", "d3-array", "d3-shape"], factory) : (global = global || self, factory(global.d3 = global.d3 || {}, global.d3, global.d3));
    })(exports, function(exports2, d3Array, d3Shape) {
      function targetDepth(d) {
        return d.target.depth;
      }
      function left(node) {
        return node.depth;
      }
      function right(node, n) {
        return n - 1 - node.height;
      }
      function justify(node, n) {
        return node.sourceLinks.length ? node.depth : n - 1;
      }
      function center(node) {
        return node.targetLinks.length ? node.depth : node.sourceLinks.length ? d3Array.min(node.sourceLinks, targetDepth) - 1 : 0;
      }
      function constant(x) {
        return function() {
          return x;
        };
      }
      function ascendingSourceBreadth(a, b) {
        return ascendingBreadth(a.source, b.source) || a.index - b.index;
      }
      function ascendingTargetBreadth(a, b) {
        return ascendingBreadth(a.target, b.target) || a.index - b.index;
      }
      function ascendingBreadth(a, b) {
        return a.y0 - b.y0;
      }
      function value(d) {
        return d.value;
      }
      function defaultId(d) {
        return d.index;
      }
      function defaultNodes(graph) {
        return graph.nodes;
      }
      function defaultLinks(graph) {
        return graph.links;
      }
      function find(nodeById, id) {
        const node = nodeById.get(id);
        if (!node) throw new Error("missing: " + id);
        return node;
      }
      function computeLinkBreadths({ nodes: nodes2 }) {
        for (const node of nodes2) {
          let y0 = node.y0;
          let y1 = y0;
          for (const link of node.sourceLinks) {
            link.y0 = y0 + link.width / 2;
            y0 += link.width;
          }
          for (const link of node.targetLinks) {
            link.y1 = y1 + link.width / 2;
            y1 += link.width;
          }
        }
      }
      function Sankey() {
        let x0 = 0, y0 = 0, x1 = 1, y1 = 1;
        let dx = 24;
        let dy = 8, py;
        let id = defaultId;
        let align = justify;
        let sort;
        let linkSort;
        let nodes2 = defaultNodes;
        let links2 = defaultLinks;
        let iterations = 6;
        function sankey2() {
          const graph = { nodes: nodes2.apply(null, arguments), links: links2.apply(null, arguments) };
          computeNodeLinks(graph);
          computeNodeValues(graph);
          computeNodeDepths(graph);
          computeNodeHeights(graph);
          computeNodeBreadths(graph);
          computeLinkBreadths(graph);
          return graph;
        }
        sankey2.update = function(graph) {
          computeLinkBreadths(graph);
          return graph;
        };
        sankey2.nodeId = function(_) {
          return arguments.length ? (id = typeof _ === "function" ? _ : constant(_), sankey2) : id;
        };
        sankey2.nodeAlign = function(_) {
          return arguments.length ? (align = typeof _ === "function" ? _ : constant(_), sankey2) : align;
        };
        sankey2.nodeSort = function(_) {
          return arguments.length ? (sort = _, sankey2) : sort;
        };
        sankey2.nodeWidth = function(_) {
          return arguments.length ? (dx = +_, sankey2) : dx;
        };
        sankey2.nodePadding = function(_) {
          return arguments.length ? (dy = py = +_, sankey2) : dy;
        };
        sankey2.nodes = function(_) {
          return arguments.length ? (nodes2 = typeof _ === "function" ? _ : constant(_), sankey2) : nodes2;
        };
        sankey2.links = function(_) {
          return arguments.length ? (links2 = typeof _ === "function" ? _ : constant(_), sankey2) : links2;
        };
        sankey2.linkSort = function(_) {
          return arguments.length ? (linkSort = _, sankey2) : linkSort;
        };
        sankey2.size = function(_) {
          return arguments.length ? (x0 = y0 = 0, x1 = +_[0], y1 = +_[1], sankey2) : [x1 - x0, y1 - y0];
        };
        sankey2.extent = function(_) {
          return arguments.length ? (x0 = +_[0][0], x1 = +_[1][0], y0 = +_[0][1], y1 = +_[1][1], sankey2) : [[x0, y0], [x1, y1]];
        };
        sankey2.iterations = function(_) {
          return arguments.length ? (iterations = +_, sankey2) : iterations;
        };
        function computeNodeLinks({ nodes: nodes3, links: links3 }) {
          for (const [i, node] of nodes3.entries()) {
            node.index = i;
            node.sourceLinks = [];
            node.targetLinks = [];
          }
          const nodeById = new Map(nodes3.map((d, i) => [id(d, i, nodes3), d]));
          for (const [i, link] of links3.entries()) {
            link.index = i;
            let { source, target } = link;
            if (typeof source !== "object") source = link.source = find(nodeById, source);
            if (typeof target !== "object") target = link.target = find(nodeById, target);
            source.sourceLinks.push(link);
            target.targetLinks.push(link);
          }
          if (linkSort != null) {
            for (const { sourceLinks, targetLinks } of nodes3) {
              sourceLinks.sort(linkSort);
              targetLinks.sort(linkSort);
            }
          }
        }
        function computeNodeValues({ nodes: nodes3 }) {
          for (const node of nodes3) {
            node.value = node.fixedValue === void 0 ? Math.max(d3Array.sum(node.sourceLinks, value), d3Array.sum(node.targetLinks, value)) : node.fixedValue;
          }
        }
        function computeNodeDepths({ nodes: nodes3 }) {
          const n = nodes3.length;
          let current = new Set(nodes3);
          let next = /* @__PURE__ */ new Set();
          let x = 0;
          while (current.size) {
            for (const node of current) {
              node.depth = x;
              for (const { target } of node.sourceLinks) {
                next.add(target);
              }
            }
            if (++x > n) throw new Error("circular link");
            current = next;
            next = /* @__PURE__ */ new Set();
          }
        }
        function computeNodeHeights({ nodes: nodes3 }) {
          const n = nodes3.length;
          let current = new Set(nodes3);
          let next = /* @__PURE__ */ new Set();
          let x = 0;
          while (current.size) {
            for (const node of current) {
              node.height = x;
              for (const { source } of node.targetLinks) {
                next.add(source);
              }
            }
            if (++x > n) throw new Error("circular link");
            current = next;
            next = /* @__PURE__ */ new Set();
          }
        }
        function computeNodeLayers({ nodes: nodes3 }) {
          const x = d3Array.max(nodes3, (d) => d.depth) + 1;
          const kx = (x1 - x0 - dx) / (x - 1);
          const columns = new Array(x);
          for (const node of nodes3) {
            const i = Math.max(0, Math.min(x - 1, Math.floor(align.call(null, node, x))));
            node.layer = i;
            node.x0 = x0 + i * kx;
            node.x1 = node.x0 + dx;
            if (columns[i]) columns[i].push(node);
            else columns[i] = [node];
          }
          if (sort) for (const column of columns) {
            column.sort(sort);
          }
          return columns;
        }
        function initializeNodeBreadths(columns) {
          const ky = d3Array.min(columns, (c) => (y1 - y0 - (c.length - 1) * py) / d3Array.sum(c, value));
          for (const nodes3 of columns) {
            let y = y0;
            for (const node of nodes3) {
              node.y0 = y;
              node.y1 = y + node.value * ky;
              y = node.y1 + py;
              for (const link of node.sourceLinks) {
                link.width = link.value * ky;
              }
            }
            y = (y1 - y + py) / (nodes3.length + 1);
            for (let i = 0; i < nodes3.length; ++i) {
              const node = nodes3[i];
              node.y0 += y * (i + 1);
              node.y1 += y * (i + 1);
            }
            reorderLinks(nodes3);
          }
        }
        function computeNodeBreadths(graph) {
          const columns = computeNodeLayers(graph);
          py = Math.min(dy, (y1 - y0) / (d3Array.max(columns, (c) => c.length) - 1));
          initializeNodeBreadths(columns);
          for (let i = 0; i < iterations; ++i) {
            const alpha = Math.pow(0.99, i);
            const beta = Math.max(1 - alpha, (i + 1) / iterations);
            relaxRightToLeft(columns, alpha, beta);
            relaxLeftToRight(columns, alpha, beta);
          }
        }
        function relaxLeftToRight(columns, alpha, beta) {
          for (let i = 1, n = columns.length; i < n; ++i) {
            const column = columns[i];
            for (const target of column) {
              let y = 0;
              let w = 0;
              for (const { source, value: value2 } of target.targetLinks) {
                let v = value2 * (target.layer - source.layer);
                y += targetTop(source, target) * v;
                w += v;
              }
              if (!(w > 0)) continue;
              let dy2 = (y / w - target.y0) * alpha;
              target.y0 += dy2;
              target.y1 += dy2;
              reorderNodeLinks(target);
            }
            if (sort === void 0) column.sort(ascendingBreadth);
            resolveCollisions(column, beta);
          }
        }
        function relaxRightToLeft(columns, alpha, beta) {
          for (let n = columns.length, i = n - 2; i >= 0; --i) {
            const column = columns[i];
            for (const source of column) {
              let y = 0;
              let w = 0;
              for (const { target, value: value2 } of source.sourceLinks) {
                let v = value2 * (target.layer - source.layer);
                y += sourceTop(source, target) * v;
                w += v;
              }
              if (!(w > 0)) continue;
              let dy2 = (y / w - source.y0) * alpha;
              source.y0 += dy2;
              source.y1 += dy2;
              reorderNodeLinks(source);
            }
            if (sort === void 0) column.sort(ascendingBreadth);
            resolveCollisions(column, beta);
          }
        }
        function resolveCollisions(nodes3, alpha) {
          const i = nodes3.length >> 1;
          const subject = nodes3[i];
          resolveCollisionsBottomToTop(nodes3, subject.y0 - py, i - 1, alpha);
          resolveCollisionsTopToBottom(nodes3, subject.y1 + py, i + 1, alpha);
          resolveCollisionsBottomToTop(nodes3, y1, nodes3.length - 1, alpha);
          resolveCollisionsTopToBottom(nodes3, y0, 0, alpha);
        }
        function resolveCollisionsTopToBottom(nodes3, y, i, alpha) {
          for (; i < nodes3.length; ++i) {
            const node = nodes3[i];
            const dy2 = (y - node.y0) * alpha;
            if (dy2 > 1e-6) node.y0 += dy2, node.y1 += dy2;
            y = node.y1 + py;
          }
        }
        function resolveCollisionsBottomToTop(nodes3, y, i, alpha) {
          for (; i >= 0; --i) {
            const node = nodes3[i];
            const dy2 = (node.y1 - y) * alpha;
            if (dy2 > 1e-6) node.y0 -= dy2, node.y1 -= dy2;
            y = node.y0 - py;
          }
        }
        function reorderNodeLinks({ sourceLinks, targetLinks }) {
          if (linkSort === void 0) {
            for (const { source: { sourceLinks: sourceLinks2 } } of targetLinks) {
              sourceLinks2.sort(ascendingTargetBreadth);
            }
            for (const { target: { targetLinks: targetLinks2 } } of sourceLinks) {
              targetLinks2.sort(ascendingSourceBreadth);
            }
          }
        }
        function reorderLinks(nodes3) {
          if (linkSort === void 0) {
            for (const { sourceLinks, targetLinks } of nodes3) {
              sourceLinks.sort(ascendingTargetBreadth);
              targetLinks.sort(ascendingSourceBreadth);
            }
          }
        }
        function targetTop(source, target) {
          let y = source.y0 - (source.sourceLinks.length - 1) * py / 2;
          for (const { target: node, width } of source.sourceLinks) {
            if (node === target) break;
            y += width + py;
          }
          for (const { source: node, width } of target.targetLinks) {
            if (node === source) break;
            y -= width;
          }
          return y;
        }
        function sourceTop(source, target) {
          let y = target.y0 - (target.targetLinks.length - 1) * py / 2;
          for (const { source: node, width } of target.targetLinks) {
            if (node === source) break;
            y += width + py;
          }
          for (const { target: node, width } of source.sourceLinks) {
            if (node === target) break;
            y -= width;
          }
          return y;
        }
        return sankey2;
      }
      function horizontalSource(d) {
        return [d.source.x1, d.y0];
      }
      function horizontalTarget(d) {
        return [d.target.x0, d.y1];
      }
      function sankeyLinkHorizontal2() {
        return d3Shape.linkHorizontal().source(horizontalSource).target(horizontalTarget);
      }
      exports2.sankey = Sankey;
      exports2.sankeyCenter = center;
      exports2.sankeyJustify = justify;
      exports2.sankeyLeft = left;
      exports2.sankeyLinkHorizontal = sankeyLinkHorizontal2;
      exports2.sankeyRight = right;
      Object.defineProperty(exports2, "__esModule", { value: true });
    });
  }
});

// node_modules/mermaid/dist/sankeyDiagram-04a897e0.js
init_polyfillShim();
var import_d3_sankey = __toESM(require_d3_sankey(), 1);
__toESM(require_dist(), 1);
__toESM(require_dayjs_min(), 1);
__toESM(require_dist2(), 1);
__toESM(require_purify(), 1);
var parser = function() {
  var o = function(k, v, o2, l) {
    for (o2 = o2 || {}, l = k.length; l--; o2[k[l]] = v)
      ;
    return o2;
  }, $V0 = [1, 9], $V1 = [1, 10], $V2 = [1, 5, 10, 12];
  var parser2 = {
    trace: function trace() {
    },
    yy: {},
    symbols_: { "error": 2, "start": 3, "SANKEY": 4, "NEWLINE": 5, "csv": 6, "opt_eof": 7, "record": 8, "csv_tail": 9, "EOF": 10, "field[source]": 11, "COMMA": 12, "field[target]": 13, "field[value]": 14, "field": 15, "escaped": 16, "non_escaped": 17, "DQUOTE": 18, "ESCAPED_TEXT": 19, "NON_ESCAPED_TEXT": 20, "$accept": 0, "$end": 1 },
    terminals_: { 2: "error", 4: "SANKEY", 5: "NEWLINE", 10: "EOF", 11: "field[source]", 12: "COMMA", 13: "field[target]", 14: "field[value]", 18: "DQUOTE", 19: "ESCAPED_TEXT", 20: "NON_ESCAPED_TEXT" },
    productions_: [0, [3, 4], [6, 2], [9, 2], [9, 0], [7, 1], [7, 0], [8, 5], [15, 1], [15, 1], [16, 3], [17, 1]],
    performAction: function anonymous(yytext, yyleng, yylineno, yy, yystate, $$, _$) {
      var $0 = $$.length - 1;
      switch (yystate) {
        case 7:
          const source = yy.findOrCreateNode($$[$0 - 4].trim().replaceAll('""', '"'));
          const target = yy.findOrCreateNode($$[$0 - 2].trim().replaceAll('""', '"'));
          const value = parseFloat($$[$0].trim());
          yy.addLink(source, target, value);
          break;
        case 8:
        case 9:
        case 11:
          this.$ = $$[$0];
          break;
        case 10:
          this.$ = $$[$0 - 1];
          break;
      }
    },
    table: [{ 3: 1, 4: [1, 2] }, { 1: [3] }, { 5: [1, 3] }, { 6: 4, 8: 5, 15: 6, 16: 7, 17: 8, 18: $V0, 20: $V1 }, { 1: [2, 6], 7: 11, 10: [1, 12] }, o($V1, [2, 4], { 9: 13, 5: [1, 14] }), { 12: [1, 15] }, o($V2, [2, 8]), o($V2, [2, 9]), { 19: [1, 16] }, o($V2, [2, 11]), { 1: [2, 1] }, { 1: [2, 5] }, o($V1, [2, 2]), { 6: 17, 8: 5, 15: 6, 16: 7, 17: 8, 18: $V0, 20: $V1 }, { 15: 18, 16: 7, 17: 8, 18: $V0, 20: $V1 }, { 18: [1, 19] }, o($V1, [2, 3]), { 12: [1, 20] }, o($V2, [2, 10]), { 15: 21, 16: 7, 17: 8, 18: $V0, 20: $V1 }, o([1, 5, 10], [2, 7])],
    defaultActions: { 11: [2, 1], 12: [2, 5] },
    parseError: function parseError(str, hash) {
      if (hash.recoverable) {
        this.trace(str);
      } else {
        var error = new Error(str);
        error.hash = hash;
        throw error;
      }
    },
    parse: function parse(input) {
      var self2 = this, stack = [0], tstack = [], vstack = [null], lstack = [], table = this.table, yytext = "", yylineno = 0, yyleng = 0, TERROR = 2, EOF = 1;
      var args = lstack.slice.call(arguments, 1);
      var lexer2 = Object.create(this.lexer);
      var sharedState = { yy: {} };
      for (var k in this.yy) {
        if (Object.prototype.hasOwnProperty.call(this.yy, k)) {
          sharedState.yy[k] = this.yy[k];
        }
      }
      lexer2.setInput(input, sharedState.yy);
      sharedState.yy.lexer = lexer2;
      sharedState.yy.parser = this;
      if (typeof lexer2.yylloc == "undefined") {
        lexer2.yylloc = {};
      }
      var yyloc = lexer2.yylloc;
      lstack.push(yyloc);
      var ranges = lexer2.options && lexer2.options.ranges;
      if (typeof sharedState.yy.parseError === "function") {
        this.parseError = sharedState.yy.parseError;
      } else {
        this.parseError = Object.getPrototypeOf(this).parseError;
      }
      function lex() {
        var token;
        token = tstack.pop() || lexer2.lex() || EOF;
        if (typeof token !== "number") {
          if (token instanceof Array) {
            tstack = token;
            token = tstack.pop();
          }
          token = self2.symbols_[token] || token;
        }
        return token;
      }
      var symbol, state, action, r, yyval = {}, p, len, newState, expected;
      while (true) {
        state = stack[stack.length - 1];
        if (this.defaultActions[state]) {
          action = this.defaultActions[state];
        } else {
          if (symbol === null || typeof symbol == "undefined") {
            symbol = lex();
          }
          action = table[state] && table[state][symbol];
        }
        if (typeof action === "undefined" || !action.length || !action[0]) {
          var errStr = "";
          expected = [];
          for (p in table[state]) {
            if (this.terminals_[p] && p > TERROR) {
              expected.push("'" + this.terminals_[p] + "'");
            }
          }
          if (lexer2.showPosition) {
            errStr = "Parse error on line " + (yylineno + 1) + ":\n" + lexer2.showPosition() + "\nExpecting " + expected.join(", ") + ", got '" + (this.terminals_[symbol] || symbol) + "'";
          } else {
            errStr = "Parse error on line " + (yylineno + 1) + ": Unexpected " + (symbol == EOF ? "end of input" : "'" + (this.terminals_[symbol] || symbol) + "'");
          }
          this.parseError(errStr, {
            text: lexer2.match,
            token: this.terminals_[symbol] || symbol,
            line: lexer2.yylineno,
            loc: yyloc,
            expected
          });
        }
        if (action[0] instanceof Array && action.length > 1) {
          throw new Error("Parse Error: multiple actions possible at state: " + state + ", token: " + symbol);
        }
        switch (action[0]) {
          case 1:
            stack.push(symbol);
            vstack.push(lexer2.yytext);
            lstack.push(lexer2.yylloc);
            stack.push(action[1]);
            symbol = null;
            {
              yyleng = lexer2.yyleng;
              yytext = lexer2.yytext;
              yylineno = lexer2.yylineno;
              yyloc = lexer2.yylloc;
            }
            break;
          case 2:
            len = this.productions_[action[1]][1];
            yyval.$ = vstack[vstack.length - len];
            yyval._$ = {
              first_line: lstack[lstack.length - (len || 1)].first_line,
              last_line: lstack[lstack.length - 1].last_line,
              first_column: lstack[lstack.length - (len || 1)].first_column,
              last_column: lstack[lstack.length - 1].last_column
            };
            if (ranges) {
              yyval._$.range = [
                lstack[lstack.length - (len || 1)].range[0],
                lstack[lstack.length - 1].range[1]
              ];
            }
            r = this.performAction.apply(yyval, [
              yytext,
              yyleng,
              yylineno,
              sharedState.yy,
              action[1],
              vstack,
              lstack
            ].concat(args));
            if (typeof r !== "undefined") {
              return r;
            }
            if (len) {
              stack = stack.slice(0, -1 * len * 2);
              vstack = vstack.slice(0, -1 * len);
              lstack = lstack.slice(0, -1 * len);
            }
            stack.push(this.productions_[action[1]][0]);
            vstack.push(yyval.$);
            lstack.push(yyval._$);
            newState = table[stack[stack.length - 2]][stack[stack.length - 1]];
            stack.push(newState);
            break;
          case 3:
            return true;
        }
      }
      return true;
    }
  };
  var lexer = /* @__PURE__ */ function() {
    var lexer2 = {
      EOF: 1,
      parseError: function parseError(str, hash) {
        if (this.yy.parser) {
          this.yy.parser.parseError(str, hash);
        } else {
          throw new Error(str);
        }
      },
      // resets the lexer, sets new input
      setInput: function(input, yy) {
        this.yy = yy || this.yy || {};
        this._input = input;
        this._more = this._backtrack = this.done = false;
        this.yylineno = this.yyleng = 0;
        this.yytext = this.matched = this.match = "";
        this.conditionStack = ["INITIAL"];
        this.yylloc = {
          first_line: 1,
          first_column: 0,
          last_line: 1,
          last_column: 0
        };
        if (this.options.ranges) {
          this.yylloc.range = [0, 0];
        }
        this.offset = 0;
        return this;
      },
      // consumes and returns one char from the input
      input: function() {
        var ch = this._input[0];
        this.yytext += ch;
        this.yyleng++;
        this.offset++;
        this.match += ch;
        this.matched += ch;
        var lines = ch.match(/(?:\r\n?|\n).*/g);
        if (lines) {
          this.yylineno++;
          this.yylloc.last_line++;
        } else {
          this.yylloc.last_column++;
        }
        if (this.options.ranges) {
          this.yylloc.range[1]++;
        }
        this._input = this._input.slice(1);
        return ch;
      },
      // unshifts one char (or a string) into the input
      unput: function(ch) {
        var len = ch.length;
        var lines = ch.split(/(?:\r\n?|\n)/g);
        this._input = ch + this._input;
        this.yytext = this.yytext.substr(0, this.yytext.length - len);
        this.offset -= len;
        var oldLines = this.match.split(/(?:\r\n?|\n)/g);
        this.match = this.match.substr(0, this.match.length - 1);
        this.matched = this.matched.substr(0, this.matched.length - 1);
        if (lines.length - 1) {
          this.yylineno -= lines.length - 1;
        }
        var r = this.yylloc.range;
        this.yylloc = {
          first_line: this.yylloc.first_line,
          last_line: this.yylineno + 1,
          first_column: this.yylloc.first_column,
          last_column: lines ? (lines.length === oldLines.length ? this.yylloc.first_column : 0) + oldLines[oldLines.length - lines.length].length - lines[0].length : this.yylloc.first_column - len
        };
        if (this.options.ranges) {
          this.yylloc.range = [r[0], r[0] + this.yyleng - len];
        }
        this.yyleng = this.yytext.length;
        return this;
      },
      // When called from action, caches matched text and appends it on next action
      more: function() {
        this._more = true;
        return this;
      },
      // When called from action, signals the lexer that this rule fails to match the input, so the next matching rule (regex) should be tested instead.
      reject: function() {
        if (this.options.backtrack_lexer) {
          this._backtrack = true;
        } else {
          return this.parseError("Lexical error on line " + (this.yylineno + 1) + ". You can only invoke reject() in the lexer when the lexer is of the backtracking persuasion (options.backtrack_lexer = true).\n" + this.showPosition(), {
            text: "",
            token: null,
            line: this.yylineno
          });
        }
        return this;
      },
      // retain first n characters of the match
      less: function(n) {
        this.unput(this.match.slice(n));
      },
      // displays already matched input, i.e. for error messages
      pastInput: function() {
        var past = this.matched.substr(0, this.matched.length - this.match.length);
        return (past.length > 20 ? "..." : "") + past.substr(-20).replace(/\n/g, "");
      },
      // displays upcoming input, i.e. for error messages
      upcomingInput: function() {
        var next = this.match;
        if (next.length < 20) {
          next += this._input.substr(0, 20 - next.length);
        }
        return (next.substr(0, 20) + (next.length > 20 ? "..." : "")).replace(/\n/g, "");
      },
      // displays the character position where the lexing error occurred, i.e. for error messages
      showPosition: function() {
        var pre = this.pastInput();
        var c = new Array(pre.length + 1).join("-");
        return pre + this.upcomingInput() + "\n" + c + "^";
      },
      // test the lexed token: return FALSE when not a match, otherwise return token
      test_match: function(match, indexed_rule) {
        var token, lines, backup;
        if (this.options.backtrack_lexer) {
          backup = {
            yylineno: this.yylineno,
            yylloc: {
              first_line: this.yylloc.first_line,
              last_line: this.last_line,
              first_column: this.yylloc.first_column,
              last_column: this.yylloc.last_column
            },
            yytext: this.yytext,
            match: this.match,
            matches: this.matches,
            matched: this.matched,
            yyleng: this.yyleng,
            offset: this.offset,
            _more: this._more,
            _input: this._input,
            yy: this.yy,
            conditionStack: this.conditionStack.slice(0),
            done: this.done
          };
          if (this.options.ranges) {
            backup.yylloc.range = this.yylloc.range.slice(0);
          }
        }
        lines = match[0].match(/(?:\r\n?|\n).*/g);
        if (lines) {
          this.yylineno += lines.length;
        }
        this.yylloc = {
          first_line: this.yylloc.last_line,
          last_line: this.yylineno + 1,
          first_column: this.yylloc.last_column,
          last_column: lines ? lines[lines.length - 1].length - lines[lines.length - 1].match(/\r?\n?/)[0].length : this.yylloc.last_column + match[0].length
        };
        this.yytext += match[0];
        this.match += match[0];
        this.matches = match;
        this.yyleng = this.yytext.length;
        if (this.options.ranges) {
          this.yylloc.range = [this.offset, this.offset += this.yyleng];
        }
        this._more = false;
        this._backtrack = false;
        this._input = this._input.slice(match[0].length);
        this.matched += match[0];
        token = this.performAction.call(this, this.yy, this, indexed_rule, this.conditionStack[this.conditionStack.length - 1]);
        if (this.done && this._input) {
          this.done = false;
        }
        if (token) {
          return token;
        } else if (this._backtrack) {
          for (var k in backup) {
            this[k] = backup[k];
          }
          return false;
        }
        return false;
      },
      // return next match in input
      next: function() {
        if (this.done) {
          return this.EOF;
        }
        if (!this._input) {
          this.done = true;
        }
        var token, match, tempMatch, index;
        if (!this._more) {
          this.yytext = "";
          this.match = "";
        }
        var rules = this._currentRules();
        for (var i = 0; i < rules.length; i++) {
          tempMatch = this._input.match(this.rules[rules[i]]);
          if (tempMatch && (!match || tempMatch[0].length > match[0].length)) {
            match = tempMatch;
            index = i;
            if (this.options.backtrack_lexer) {
              token = this.test_match(tempMatch, rules[i]);
              if (token !== false) {
                return token;
              } else if (this._backtrack) {
                match = false;
                continue;
              } else {
                return false;
              }
            } else if (!this.options.flex) {
              break;
            }
          }
        }
        if (match) {
          token = this.test_match(match, rules[index]);
          if (token !== false) {
            return token;
          }
          return false;
        }
        if (this._input === "") {
          return this.EOF;
        } else {
          return this.parseError("Lexical error on line " + (this.yylineno + 1) + ". Unrecognized text.\n" + this.showPosition(), {
            text: "",
            token: null,
            line: this.yylineno
          });
        }
      },
      // return next match that has a token
      lex: function lex() {
        var r = this.next();
        if (r) {
          return r;
        } else {
          return this.lex();
        }
      },
      // activates a new lexer condition state (pushes the new lexer condition state onto the condition stack)
      begin: function begin(condition) {
        this.conditionStack.push(condition);
      },
      // pop the previously active lexer condition state off the condition stack
      popState: function popState() {
        var n = this.conditionStack.length - 1;
        if (n > 0) {
          return this.conditionStack.pop();
        } else {
          return this.conditionStack[0];
        }
      },
      // produce the lexer rule set which is active for the currently active lexer condition state
      _currentRules: function _currentRules() {
        if (this.conditionStack.length && this.conditionStack[this.conditionStack.length - 1]) {
          return this.conditions[this.conditionStack[this.conditionStack.length - 1]].rules;
        } else {
          return this.conditions["INITIAL"].rules;
        }
      },
      // return the currently active lexer condition state; when an index argument is provided it produces the N-th previous condition state, if available
      topState: function topState(n) {
        n = this.conditionStack.length - 1 - Math.abs(n || 0);
        if (n >= 0) {
          return this.conditionStack[n];
        } else {
          return "INITIAL";
        }
      },
      // alias for begin(condition)
      pushState: function pushState(condition) {
        this.begin(condition);
      },
      // return the number of states currently on the stack
      stateStackSize: function stateStackSize() {
        return this.conditionStack.length;
      },
      options: { "case-insensitive": true },
      performAction: function anonymous(yy, yy_, $avoiding_name_collisions, YY_START) {
        switch ($avoiding_name_collisions) {
          case 0:
            this.pushState("csv");
            return 4;
          case 1:
            return 10;
          case 2:
            return 5;
          case 3:
            return 12;
          case 4:
            this.pushState("escaped_text");
            return 18;
          case 5:
            return 20;
          case 6:
            this.popState("escaped_text");
            return 18;
          case 7:
            return 19;
        }
      },
      rules: [/^(?:sankey-beta\b)/i, /^(?:$)/i, /^(?:((\u000D\u000A)|(\u000A)))/i, /^(?:(\u002C))/i, /^(?:(\u0022))/i, /^(?:([\u0020-\u0021\u0023-\u002B\u002D-\u007E])*)/i, /^(?:(\u0022)(?!(\u0022)))/i, /^(?:(([\u0020-\u0021\u0023-\u002B\u002D-\u007E])|(\u002C)|(\u000D)|(\u000A)|(\u0022)(\u0022))*)/i],
      conditions: { "csv": { "rules": [1, 2, 3, 4, 5, 6, 7], "inclusive": false }, "escaped_text": { "rules": [6, 7], "inclusive": false }, "INITIAL": { "rules": [0, 1, 2, 3, 4, 5, 6, 7], "inclusive": true } }
    };
    return lexer2;
  }();
  parser2.lexer = lexer;
  function Parser() {
    this.yy = {};
  }
  Parser.prototype = parser2;
  parser2.Parser = Parser;
  return new Parser();
}();
parser.parser = parser;
var parser$1 = parser;
var links = [];
var nodes = [];
var nodesMap = {};
var clear2 = () => {
  links = [];
  nodes = [];
  nodesMap = {};
  clear();
};
var SankeyLink = class {
  constructor(source, target, value = 0) {
    this.source = source;
    this.target = target;
    this.value = value;
  }
};
var addLink = (source, target, value) => {
  links.push(new SankeyLink(source, target, value));
};
var SankeyNode = class {
  constructor(ID) {
    this.ID = ID;
  }
};
var findOrCreateNode = (ID) => {
  ID = common$1.sanitizeText(ID, getConfig());
  if (!nodesMap[ID]) {
    nodesMap[ID] = new SankeyNode(ID);
    nodes.push(nodesMap[ID]);
  }
  return nodesMap[ID];
};
var getNodes = () => nodes;
var getLinks = () => links;
var getGraph = () => ({
  nodes: nodes.map((node) => ({ id: node.ID })),
  links: links.map((link) => ({
    source: link.source.ID,
    target: link.target.ID,
    value: link.value
  }))
});
var db = {
  nodesMap,
  getConfig: () => getConfig().sankey,
  getNodes,
  getLinks,
  getGraph,
  addLink,
  findOrCreateNode,
  getAccTitle,
  setAccTitle,
  getAccDescription,
  setAccDescription,
  getDiagramTitle,
  setDiagramTitle,
  clear: clear2
};
var _Uid = class _Uid2 {
  static next(name) {
    return new _Uid2(name + ++_Uid2.count);
  }
  constructor(id) {
    this.id = id;
    this.href = `#${id}`;
  }
  toString() {
    return "url(" + this.href + ")";
  }
};
_Uid.count = 0;
var Uid = _Uid;
var alignmentsMap = {
  left: import_d3_sankey.sankeyLeft,
  right: import_d3_sankey.sankeyRight,
  center: import_d3_sankey.sankeyCenter,
  justify: import_d3_sankey.sankeyJustify
};
var draw = function(text, id, _version, diagObj) {
  const { securityLevel, sankey: conf } = getConfig();
  const defaultSankeyConfig = defaultConfig.sankey;
  let sandboxElement;
  if (securityLevel === "sandbox") {
    sandboxElement = select_default("#i" + id);
  }
  const root = securityLevel === "sandbox" ? select_default(sandboxElement.nodes()[0].contentDocument.body) : select_default("body");
  const svg = securityLevel === "sandbox" ? root.select(`[id="${id}"]`) : select_default(`[id="${id}"]`);
  const width = (conf == null ? void 0 : conf.width) ?? defaultSankeyConfig.width;
  const height = (conf == null ? void 0 : conf.height) ?? defaultSankeyConfig.width;
  const useMaxWidth = (conf == null ? void 0 : conf.useMaxWidth) ?? defaultSankeyConfig.useMaxWidth;
  const nodeAlignment = (conf == null ? void 0 : conf.nodeAlignment) ?? defaultSankeyConfig.nodeAlignment;
  const prefix = (conf == null ? void 0 : conf.prefix) ?? defaultSankeyConfig.prefix;
  const suffix = (conf == null ? void 0 : conf.suffix) ?? defaultSankeyConfig.suffix;
  const showValues = (conf == null ? void 0 : conf.showValues) ?? defaultSankeyConfig.showValues;
  const graph = diagObj.db.getGraph();
  const nodeAlign = alignmentsMap[nodeAlignment];
  const nodeWidth = 10;
  const sankey$1 = (0, import_d3_sankey.sankey)().nodeId((d) => d.id).nodeWidth(nodeWidth).nodePadding(10 + (showValues ? 15 : 0)).nodeAlign(nodeAlign).extent([
    [0, 0],
    [width, height]
  ]);
  sankey$1(graph);
  const colorScheme = ordinal(Tableau10_default);
  svg.append("g").attr("class", "nodes").selectAll(".node").data(graph.nodes).join("g").attr("class", "node").attr("id", (d) => (d.uid = Uid.next("node-")).id).attr("transform", function(d) {
    return "translate(" + d.x0 + "," + d.y0 + ")";
  }).attr("x", (d) => d.x0).attr("y", (d) => d.y0).append("rect").attr("height", (d) => {
    return d.y1 - d.y0;
  }).attr("width", (d) => d.x1 - d.x0).attr("fill", (d) => colorScheme(d.id));
  const getText = ({ id: id2, value }) => {
    if (!showValues) {
      return id2;
    }
    return `${id2}
${prefix}${Math.round(value * 100) / 100}${suffix}`;
  };
  svg.append("g").attr("class", "node-labels").attr("font-family", "sans-serif").attr("font-size", 14).selectAll("text").data(graph.nodes).join("text").attr("x", (d) => d.x0 < width / 2 ? d.x1 + 6 : d.x0 - 6).attr("y", (d) => (d.y1 + d.y0) / 2).attr("dy", `${showValues ? "0" : "0.35"}em`).attr("text-anchor", (d) => d.x0 < width / 2 ? "start" : "end").text(getText);
  const link = svg.append("g").attr("class", "links").attr("fill", "none").attr("stroke-opacity", 0.5).selectAll(".link").data(graph.links).join("g").attr("class", "link").style("mix-blend-mode", "multiply");
  const linkColor = (conf == null ? void 0 : conf.linkColor) || "gradient";
  if (linkColor === "gradient") {
    const gradient = link.append("linearGradient").attr("id", (d) => (d.uid = Uid.next("linearGradient-")).id).attr("gradientUnits", "userSpaceOnUse").attr("x1", (d) => d.source.x1).attr("x2", (d) => d.target.x0);
    gradient.append("stop").attr("offset", "0%").attr("stop-color", (d) => colorScheme(d.source.id));
    gradient.append("stop").attr("offset", "100%").attr("stop-color", (d) => colorScheme(d.target.id));
  }
  let coloring;
  switch (linkColor) {
    case "gradient":
      coloring = (d) => d.uid;
      break;
    case "source":
      coloring = (d) => colorScheme(d.source.id);
      break;
    case "target":
      coloring = (d) => colorScheme(d.target.id);
      break;
    default:
      coloring = linkColor;
  }
  link.append("path").attr("d", (0, import_d3_sankey.sankeyLinkHorizontal)()).attr("stroke", coloring).attr("stroke-width", (d) => Math.max(1, d.width));
  setupGraphViewbox$1(void 0, svg, 0, useMaxWidth);
};
var renderer = {
  draw
};
var prepareTextForParsing = (text) => {
  const textToParse = text.replaceAll(/^[^\S\n\r]+|[^\S\n\r]+$/g, "").replaceAll(/([\n\r])+/g, "\n").trim();
  return textToParse;
};
var originalParse = parser$1.parse.bind(parser$1);
parser$1.parse = (text) => originalParse(prepareTextForParsing(text));
var diagram = {
  parser: parser$1,
  db,
  renderer
};

export { diagram };
//# sourceMappingURL=sankeyDiagram-04a897e0-6L2YHKMN.js.map
//# sourceMappingURL=sankeyDiagram-04a897e0-6L2YHKMN.js.map