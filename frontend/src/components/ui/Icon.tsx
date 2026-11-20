import type { LucideIcon } from 'lucide-react';
import type { SVGProps } from 'react';

interface IconProps extends SVGProps<SVGSVGElement> {
  icon: LucideIcon;
  size?: number;
}

/** Wraps lucide icons with sharp (square) caps and miter joins per the design. */
export function Icon({ icon: IconComponent, size = 20, ...props }: IconProps) {
  return (
    <IconComponent
      width={size}
      height={size}
      strokeLinecap="square"
      strokeLinejoin="miter"
      aria-hidden="true"
      {...props}
    />
  );
}
